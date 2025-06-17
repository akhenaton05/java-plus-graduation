package ru.practicum.events.service;

import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.controller.RecommendationsClient;
import ru.practicum.controller.UserActionClient;
import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.dto.SearchEventsParams;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.request_service.feign.RequestClient;
import ru.practicum.user_service.config.DateConfig;
import ru.practicum.errors.EventNotPublishedException;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.QEvent;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.request_service.entity.ParticipationRequestStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicEventsServiceImpl implements PublicEventsService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;
    private final RecommendationsClient recommendationsClient;
    private final UserActionClient collectorClient;

    @Override
    public Event getEvent(Long id) {
        return eventRepository.findEventWithStatus(id, ParticipationRequestStatus.CONFIRMED);
    }

    @Override
    public EventFullDto getEventAnyStatusWithViews(Long id) {
        Event event = eventRepository.getSingleEvent(id);
        if (Objects.isNull(event)) {
                throw new EntityNotFoundException("Event with " + id + " not found");
        }
        if (!event.getState().equals(StateEvent.PUBLISHED)) {
            throw new EventNotPublishedException("There is no published event id " + event.getId());
        }
        // Получаем рейтинг через gRPC
        event.setRating(getEventRating(id));
        return eventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto getEventById(Long id) {
        return eventMapper.toEventFullDto(eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event with " + id + " not found")));
    }

    public List<Event> getEventsByListIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids))
            return List.of();

        List<Event> events = eventRepository.findEventsWithConfirmedCount(ids);
        if (CollectionUtils.isEmpty(events))
            return events;

        // Запрашиваем рейтинги для всех событий через gRPC
        Map<Long, Double> ratings = getEventRatings(ids);
        events.forEach(event -> event.setRating(ratings.getOrDefault(event.getId(), 0.0)));

        return events;
    }

    @Override
    public EventFullDto getEventInfo(Long eventId, Long userId) {
        log.info("\nPublicEventsServiceImpl.getEventInfo: accepted {}", eventId);
        Event event = getEvent(eventId);
        log.info("\nPublicEventsServiceImpl.getEventsViews: event {}", event);
        if (!event.getState().equals(StateEvent.PUBLISHED)) {
            throw new EventNotPublishedException("There is no published event id " + event.getId());
        }

        event.setRating(getEventRating(eventId)); // Получаем рейтинг через gRPC
        // Отправляем информацию о просмотре через gRPC
        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW, Instant.now());

        return eventMapper.toEventFullDto(event);
    }

    @Override
    public List<EventShortDto> getFilteredEvents(SearchEventsParams searchEventsParams) {
        BooleanBuilder builder = new BooleanBuilder();

        if (!Strings.isEmpty(searchEventsParams.getText())) {
            builder.or(QEvent.event.annotation.containsIgnoreCase(searchEventsParams.getText()))
                    .or(QEvent.event.description.containsIgnoreCase(searchEventsParams.getText()));
        }

        builder.and(QEvent.event.state.eq(StateEvent.PUBLISHED));

        if (!CollectionUtils.isEmpty(searchEventsParams.getCategories())) {
            builder.and(QEvent.event.category.id.in(searchEventsParams.getCategories()));
        }

        if (Objects.nonNull(searchEventsParams.getPaid())) {
            builder.and(QEvent.event.paid.eq(searchEventsParams.getPaid()));
        }

        LocalDateTime start;
        if (Objects.isNull(searchEventsParams.getRangeStart())) {
            start = LocalDateTime.now();
            searchEventsParams.setRangeStart(start.format(DateConfig.FORMATTER));
        } else {
            start = LocalDateTime.parse(searchEventsParams.getRangeStart(), DateConfig.FORMATTER);
        }

        if (Objects.isNull(searchEventsParams.getRangeEnd())) {
            builder.and(QEvent.event.eventDate.goe(start));
        } else {
            LocalDateTime end = LocalDateTime.parse(searchEventsParams.getRangeEnd(), DateConfig.FORMATTER);
            builder.and(QEvent.event.eventDate.between(start, end));
        }

        List<Event> events = eventRepository.searchEvents(builder, ParticipationRequestStatus.CONFIRMED,
                searchEventsParams.getOnlyAvailable(), searchEventsParams.getFrom(), searchEventsParams.getSize());

        if (events.isEmpty()) {
            return List.of();
        }

        // Запрашиваем рейтинги для всех событий через gRPC
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Double> ratings = getEventRatings(eventIds);
        events.forEach(event -> event.setRating(ratings.getOrDefault(event.getId(), 0.0)));

        String sortParam = Strings.isEmpty(searchEventsParams.getSort()) ? "RATING" : searchEventsParams.getSort().toUpperCase();
        List<Event> sortedEvents;
        if (sortParam.equalsIgnoreCase("EVENT_DATE")) {
            sortedEvents = events.stream()
                    .sorted(Comparator.comparing(Event::getEventDate))
                    .toList();
        } else {
            sortedEvents = events.stream()
                    .sorted(Comparator.comparingDouble(Event::getRating).reversed())
                    .toList();
        }

        return eventMapper.toListEventShortDto(sortedEvents);
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {
        if (requestClient.existsByUserIdAndEventId(userId, eventId)) {
            log.info("Отправка регистрации на мероприятие в collector");
            collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE, Instant.now());
        } else {
            throw new NotFoundException(String.format("Пользователь userId {} не зарегистрирован на событие eventId {}", userId, eventId));
        }
    }

    private double getEventRating(Long eventId) {
        Stream<RecommendedEventProto> interactions = recommendationsClient.getInteractionsCount(List.of(eventId));
        return interactions
                .filter(proto -> proto.getEventId() == eventId)
                .findFirst()
                .map(RecommendedEventProto::getScore) // Предполагаем, что score — это рейтинг
                .orElse(0.0);
    }

    private Map<Long, Double> getEventRatings(List<Long> eventIds) {
        Stream<RecommendedEventProto> interactions = recommendationsClient.getInteractionsCount(eventIds);
        return interactions
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore,
                        (existing, replacement) -> existing)); // В случае дубликатов сохраняем первый
    }

}