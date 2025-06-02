package ru.practicum.events.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.mapper.CategoryDtoMapper;
import ru.practicum.category.model.Category;
import ru.practicum.config.DateConfig;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.NewEventDto;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.StateEvent;
import ru.practicum.users.dto.UserShortDto;
import ru.practicum.users.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EventMapper {
    private final CategoryDtoMapper categoryDtoMapper;

    public static NewEventDto toNewEventDto(Event event) {
        return NewEventDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(event.getCategory().getId())
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(DateConfig.FORMATTER))
                .location(event.getLocation())
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .title(event.getTitle())
                .build();
    }

    public static Event dtoToEvent(NewEventDto dto, User user) {
        Category category = new Category();
        category.setId((long) dto.getCategory());

        LocalDateTime eventTime = LocalDateTime.parse(dto.getEventDate(), DateConfig.FORMATTER);
        return Event.builder()
                .id(dto.getId())
                .annotation(dto.getAnnotation())
                .title(dto.getTitle())
                .category(category)
                .description(dto.getDescription())
                .eventDate(eventTime)
                .location(dto.getLocation())
                .paid(dto.isPaid())
                .participantLimit(Objects.nonNull(dto.getParticipantLimit()) ? dto.getParticipantLimit() : 0)
                .requestModeration(Objects.nonNull(dto.getRequestModeration()) ? dto.getRequestModeration() : true)
                .initiator(user)
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now())
                .state(StateEvent.PENDING)
                .views(0)
                .build();
    }

    public EventFullDto toEventFullDto(Event event) {
        String publishedOn = Objects.isNull(event.getPublishedOn()) ?
                null :
                event.getPublishedOn().format(DateConfig.FORMATTER);

        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDtoMapper.mapCategoryToDto(event.getCategory()))
                .confirmedRequests((Objects.isNull(event.getConfirmedRequests())) ? 0 : event.getConfirmedRequests())
                .eventDate(event.getEventDate().format(DateConfig.FORMATTER))
                .initiator(new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()))
                .paid(event.isPaid())
                .title(event.getTitle())
                .views((Objects.isNull(event.getViews())) ? 0 : event.getViews())
                .createdOn(event.getCreatedOn().format(DateConfig.FORMATTER))
                .description(event.getDescription())
                .location(event.getLocation())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(publishedOn)
                .requestModeration(event.isRequestModeration())
                .state(event.getState())
                .build();
    }

    public EventShortDto toEventShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDtoMapper.mapCategoryToDto(event.getCategory()))
                .confirmedRequests((Objects.isNull(event.getConfirmedRequests())) ? 0 : event.getConfirmedRequests())
                .eventDate(event.getEventDate().format(DateConfig.FORMATTER))
                .initiator(new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()))
                .paid(event.isPaid())
                .title(event.getTitle())
                .views((Objects.isNull(event.getViews())) ? 0 : event.getViews())
                .build();
    }

    public List<EventShortDto> toListEventShortDto(List<Event> events) {
        return events.stream()
                .map(this::toEventShortDto)
                .toList();
    }

}