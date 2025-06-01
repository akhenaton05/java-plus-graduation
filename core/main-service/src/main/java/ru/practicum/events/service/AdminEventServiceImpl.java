package ru.practicum.events.service;

import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.UpdateEventAdminRequest;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventStateAction;
import ru.practicum.events.model.QEvent;
import ru.practicum.events.model.StateEvent;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.validation.AdminEventValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;

    @Override
    public List<EventFullDto> getEvents(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size) {

        // Создаем объект QEvent для построения запроса
        QEvent event = QEvent.event;

        // Строим базовый запрос
        BooleanBuilder builder = new BooleanBuilder();

        // Фильтрация по пользователям
        if (!CollectionUtils.isEmpty(users)) {
            builder.and(event.initiator.id.in(users));
        }

        // Фильтрация по состояниям
        if (!CollectionUtils.isEmpty(states)) {
            builder.and(event.state.in(states.stream().map(StateEvent::valueOf).collect(Collectors.toList())));
        }

        // Фильтрация по категориям
        if (!CollectionUtils.isEmpty(categories)) {
            builder.and(event.category.id.in(categories));
        }

        // Фильтрация по диапазону времени
        if (Objects.nonNull(rangeStart)) {
            builder.and(event.eventDate.goe(rangeStart));
        }

        if (Objects.nonNull(rangeEnd)) {
            builder.and(event.eventDate.loe(rangeEnd));
        }

        // Пагинация
        Pageable pageable = PageRequest.of(from / size, size);

        // Выполнение запроса
        Page<Event> eventsPage = eventRepository.findAllWithBuilder(builder, pageable);

        // Получаем список ID событий
        List<Long> eventIds = eventsPage.getContent().stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // Получаем события с количеством подтвержденных запросов
        List<Event> eventsWithConfirmedRequests = eventRepository.findEventsWithConfirmedCount(eventIds);

        // Преобразуем сущности Event в EventFullDto
        return eventsPage.getContent().stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        // 1. Найти событие
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with id=" + eventId + " was not found"));

        // 2. Проверки перед обновлением
        AdminEventValidator.validateEventStatusUpdate(event, updateRequest);

        // 3. Обновление данных события
        if (Objects.nonNull(updateRequest.getTitle())) {
            event.setTitle(updateRequest.getTitle());
        }
        if (Objects.nonNull(updateRequest.getAnnotation())) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (Objects.nonNull(updateRequest.getDescription())) {
            event.setDescription(updateRequest.getDescription());
        }
        if (Objects.nonNull(updateRequest.getEventDate())) {
            event.setEventDate(updateRequest.getEventDate());
        }
        if (Objects.nonNull(updateRequest.getCategory())) {
            Category category = categoryRepository
                    .findById(Long.valueOf(updateRequest.getCategory()))
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Category with id=" + updateRequest.getCategory() + " not found"));

            event.setCategory(category);
        }
        if (Objects.nonNull(updateRequest.getPaid())) {
            event.setPaid(updateRequest.getPaid());
        }
        if (Objects.nonNull(updateRequest.getParticipantLimit())) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (Objects.nonNull(updateRequest.getStateAction())) {
            updateEventState(event, updateRequest.getStateAction());
        }

        // 4. Сохранение изменений
        event = eventRepository.save(event);

        // 5. Преобразование в DTO и возврат
        return eventMapper.toEventFullDto(event);
    }

    private void updateEventState(Event event, EventStateAction stateAction) {
        if (stateAction.equals(EventStateAction.PUBLISH_EVENT)) {
            event.setState(StateEvent.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (stateAction.equals(EventStateAction.REJECT_EVENT)) {
            event.setState(StateEvent.CANCELED);
        }
    }
}
