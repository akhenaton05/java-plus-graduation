package ru.practicum.events.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import feign.FeignException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.QEvent;
import ru.practicum.request_service.entity.ParticipationRequestStatus;
import ru.practicum.request_service.feign.RequestClient;

import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EventRepositoryCustomImpl implements EventRepositoryCustom {
    private final EntityManager em;
    private final RequestClient requestClient;

    @Override
    public Page<Event> findAllWithBuilder(BooleanBuilder builder, Pageable pageable) {
        QEvent event = QEvent.event;

        // Запрос для получения данных с пагинацией и сортировкой
        JPAQuery<Event> query = new JPAQuery<>(em)
                .select(event)
                .from(event)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
        List<Event> content = query.fetch();

        // Запрос для подсчета общего количества элементов (используем count)
        Long total = new JPAQuery<>(em).select(event.count())
                .from(event)
                .fetchOne();

        if (Objects.isNull(total))
            throw new RuntimeException("Не удалось определить количество событий");

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Event findEventWithStatus(Long eventId, ParticipationRequestStatus status) {
        QEvent event = QEvent.event;

        Event foundEvent = new JPAQuery<>(em)
                .select(event)
                .from(event)
                .where(event.id.eq(eventId))
                .fetchOne();

        if (foundEvent == null) {
            throw new EntityNotFoundException("Event with id " + eventId + " not found");
        }

        Integer confirmedCount = 0;
        if (status == ParticipationRequestStatus.CONFIRMED) {
            try {
                ResponseEntity<Integer> response = requestClient.getConfirmedRequestsCount(eventId);
                confirmedCount = response.getBody() != null ? response.getBody() : 0;
            } catch (FeignException e) {
                log.error("Failed to fetch confirmed requests count for event {}: {}", eventId, e.getMessage());
                confirmedCount = 0;
            }
        }

        foundEvent.setConfirmedRequests(confirmedCount);
        log.info("STATUS IS {}", status);
        return foundEvent;
    }

    @Override
    public List<Event> searchEvents(BooleanBuilder eventCondition, ParticipationRequestStatus status,
                                    boolean onlyAvailable, int from, int size) {
        log.info("1");
        QEvent event = QEvent.event;

        log.info("2");
        // Получаем события
        List<Event> events = new JPAQuery<>(em)
                .select(event)
                .from(event)
                .where(eventCondition)
                .fetch();

        log.info("3");
        // Получаем количество подтверждённых заявок
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Integer> confirmedCounts = eventIds.isEmpty() ? new HashMap<>()
                : requestClient.getConfirmedRequestsCounts(eventIds).getBody();
        events.forEach(e -> e.setConfirmedRequests(confirmedCounts.getOrDefault(e.getId(), 0)));

        log.info("4");
        // Фильтруем по onlyAvailable
        if (onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            e.getParticipantLimit() > e.getConfirmedRequests())
                    .toList();
        }

        log.info("5");
        // Применяем пагинацию
        int toIndex = Math.min(from + size, events.size());
        if (from >= events.size()) {
            return List.of();
        }
        return events.subList(from, toIndex);
    }

    @Override
    public List<Event> findEventsWithConfirmedCount(List<Long> eventIds) {
        QEvent event = QEvent.event;

        // Получаем события из базы данных
        List<Event> events = new JPAQuery<>(em)
                .select(event)
                .from(event)
                .where(event.id.in(eventIds))
                .fetch();

        // Получаем количество подтверждённых заявок через Feign-клиент
        Map<Long, Integer> confirmedCounts = eventIds.isEmpty() ? new HashMap<>()
                : requestClient.getConfirmedRequestsCounts(eventIds).getBody();

        // Устанавливаем количество подтверждённых заявок для каждого события
        events.forEach(e -> e.setConfirmedRequests(confirmedCounts.getOrDefault(e.getId(), 0)));

        return events;
    }

    @Override
    public Event getSingleEvent(Long id) {
        QEvent event = QEvent.event;

        Event eventResult = new JPAQuery<>(em)
                .select(event)
                .from(event)
                .where(event.id.eq(id))
                .fetchOne();

        if (eventResult == null) {
            return null;
        }

        // Получаем количество подтверждённых заявок через Feign-клиент
        Integer confirmedCount = requestClient.getConfirmedRequestsCount(id).getBody();
        eventResult.setConfirmedRequests(confirmedCount != null ? confirmedCount : 0);

        return eventResult;
    }

    private List<Event> tuplesToEvents(QEvent event, List<Tuple> tuples) {
        List<Event> events = new ArrayList<>();
        for (Tuple tuple : tuples) {
            Event e = tuple.get(event);  // Извлекаем событие
            if (Objects.nonNull(e)) {
                Integer confirmedCount = Optional.ofNullable(tuple.get(1, Long.class))
                        .map(Long::intValue)
                        .orElse(0);  // Извлекаем количество участников
                e.setConfirmedRequests(confirmedCount);
                events.add(e);
            }
        }
        return events;
    }
}
