package ru.practicum.events.service;

import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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
import ru.practicum.user_service.config.StatsClientConfig;
import ru.practicum.dto.ReadEndpointHitDto;
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
public class PublicEventsServiceImpl implements PublicEventsService {

    private final EventRepository eventRepository;
    //private final ClientController clientController;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;
    private final RecommendationsClient recommendationsClient;
    private final UserActionClient collectorClient;

    @Autowired
    public PublicEventsServiceImpl(EventRepository eventRepository,
                                   DiscoveryClient discoveryClient,
                                   StatsClientConfig statsClientConfig,
                                   EventMapper eventMapper,
                                   RequestClient requestClient,
                                   RecommendationsClient recommendationsClient,
                                   UserActionClient collectorClient) {
        this.eventRepository = eventRepository;
        //this.clientController = new ClientController(discoveryClient, statsClientConfig.getServiceId());
        this.eventMapper = eventMapper;
        this.requestClient = requestClient;
        this.recommendationsClient = recommendationsClient;
        this.collectorClient = collectorClient;
    }

    @Override
    public Event getEvent(Long id) {
        return eventRepository.findEventWithStatus(id, ParticipationRequestStatus.CONFIRMED);
    }
//
//    @Override
//    public int getEventsViews(long id, LocalDateTime publishedOn) {
//        List<String> uris = List.of("/events/" + id);
//        List<ReadEndpointHitDto> res = clientController.getHits(publishedOn.format(DateConfig.FORMATTER),
//                LocalDateTime.now().format(DateConfig.FORMATTER), uris, true);
//        log.info("\nPublicEventsServiceImpl.getEventsViews: res {}", res);
//        return (CollectionUtils.isEmpty(res)) ? 0 : res.getFirst().getHits();
//    }

    @Override
    public EventFullDto getEventAnyStatusWithViews(Long id) {
        //Attention: this method works without saving views!
        Event event = eventRepository.getSingleEvent(id);
        if (Objects.isNull(event)) {
                throw new EntityNotFoundException("Event with " + id + " not found");
        }
        if (!event.getState().equals(StateEvent.PUBLISHED)) {
            throw new EventNotPublishedException("There is no published event id " + event.getId());
        }
        // Получаем рейтинг через gRPC
        event.setRating(getEventRating(id));
//        event.setViews(getEventsViews(event.getId(), event.getPublishedOn()));
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
//        LocalDateTime start = events.stream()
//                .map(Event::getPublishedOn)
//                .min(LocalDateTime::compareTo)
//                .orElseThrow(() ->
//                        new RuntimeException("Internal server error during execution PublicEventsServiceImpl"));
//        List<String> uris = events.stream()
//                .map(event -> "/event/" + event.getId())
//                .toList();
//
//        List<ReadEndpointHitDto> acceptedList = clientController.getHits(start.format(DateConfig.FORMATTER),
//                LocalDateTime.now().format(DateConfig.FORMATTER), uris, true);
        // Заносим значения views в список events
//        viewsToEvents(acceptedList, events);
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
//        // Получаем views
//        event.setViews(getEventsViews(event.getId(), event.getPublishedOn()));
//        //Имеем новый просмотр - сохраняем его
//        clientController.saveView(lookEventDto.getIp(), lookEventDto.getUri());

        return eventMapper.toEventFullDto(event);
    }

//    @Override
//    public List<EventShortDto> getFilteredEvents(SearchEventsParams searchEventsParams) {
//        log.info("\nPublicEventsServiceImpl.getFilteredEvents: {}", searchEventsParams);
//
//        BooleanBuilder builder = new BooleanBuilder();
//
//        // Добавляем условия отбора по контексту
//        if (!Strings.isEmpty(searchEventsParams.getText())) {
//            builder.or(QEvent.event.annotation.containsIgnoreCase(searchEventsParams.getText()))
//                    .or(QEvent.event.description.containsIgnoreCase(searchEventsParams.getText()));
//        }
//
//        // Добавляем отбор по статусу PUBLISHED
//        builder.and(QEvent.event.state.eq(StateEvent.PUBLISHED));
//        // ... и по списку категорий
//        if (!CollectionUtils.isEmpty(searchEventsParams.getCategories()))
//            builder.and(QEvent.event.category.id.in(searchEventsParams.getCategories()));
//
//        // ... и еще по признаку платные/бесплатные
//        if (Objects.nonNull(searchEventsParams.getPaid()))
//            builder.and(QEvent.event.paid.eq(searchEventsParams.getPaid()));
//
//        // Добавляем условие диапазона дат
//        LocalDateTime start;
//        LocalDateTime end;
//        if (Objects.isNull(searchEventsParams.getRangeStart())) {
//            start = LocalDateTime.now();
//            searchEventsParams.setRangeStart(start.format(DateConfig.FORMATTER));
//        } else {
//            start = LocalDateTime.parse(searchEventsParams.getRangeStart(), DateConfig.FORMATTER);
//        }
//        if (Objects.isNull(searchEventsParams.getRangeEnd())) {
//            builder.and(QEvent.event.eventDate.goe(start));
//        } else {
//            end = LocalDateTime.parse(searchEventsParams.getRangeEnd(), DateConfig.FORMATTER);
//            builder.and(QEvent.event.eventDate.between(start, end));
//        }
//
//        List<Event> events = eventRepository.searchEvents(builder, ParticipationRequestStatus.CONFIRMED,
//                searchEventsParams.getOnlyAvailable(), searchEventsParams.getFrom(), searchEventsParams.getSize());
//
////        if (events.isEmpty()) {
////            clientController.saveView(lookEventDto.getIp(), "/events");
////            return List.of();
////        }
//
//        log.info("PublicEventsServiceImpl.getFilteredEvents: events {}", events);
//        // Если не было установлено rangeEnd, устанавливаем
//        if (Objects.isNull(searchEventsParams.getRangeEnd())) {
//            searchEventsParams.setRangeEnd(LocalDateTime.now().format(DateConfig.FORMATTER));
//        }
//        // Формируем список uris
//        List<String> uris = new ArrayList<>();
//        for (Event e : events) {
//            uris.add("/events/" + e.getId());
//        }
//
//        List<ReadEndpointHitDto> acceptedList = clientController.getHits(searchEventsParams.getRangeStart(),
//                searchEventsParams.getRangeEnd(), uris, true);
////        viewsToEvents(acceptedList, events);
//
//        // Сортировка. Для начала проверяем значение параметра сортировки
//        String sortParam;
//        if (Strings.isEmpty(searchEventsParams.getSort())) {
//            sortParam = "VIEWS";
//        } else {
//            sortParam = searchEventsParams.getSort().toUpperCase();
//        }
//        // Дополняем сортировкой
//        List<Event> sortedEvents = new ArrayList<>();
//        if (sortParam.equalsIgnoreCase("EVENT_DATE")) {
//            sortedEvents = events.stream()
//                    .sorted(Comparator.comparing(Event::getEventDate)) // Сортируем по eventDate
//                    .toList();
//        } else {
//            sortedEvents = events.stream()
//                    .sorted(Comparator.comparingInt(Event::getViews).reversed()) // Сортируем по views
//                    .toList();
//        }
//
////        uris.add("/events");
////        clientController.saveHitsGroup(uris, lookEventDto.getIp());
//        log.info("\n Final list {}", sortedEvents);
//        return eventMapper.toListEventShortDto(sortedEvents);
//    }

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

//    public void viewsToEvents(List<ReadEndpointHitDto> viewsList, List<Event> events) {
//        // Заносим значения views в список events
//        Map<Integer, Integer> workMap = new HashMap<>();
//        for (ReadEndpointHitDto r : viewsList) {
//            int i = Integer.parseInt(r.getUri().substring(r.getUri().lastIndexOf("/") + 1));
//            workMap.put(i, r.getHits());
//        }
//        for (Event e : events) {
//            e.setViews(workMap.getOrDefault(e.getId(), 0));
//        }
//    }
}