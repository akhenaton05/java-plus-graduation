package ru.practicum.events.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.category.mapper.CategoryDtoMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.dto.LocationDto;
import ru.practicum.event_service.dto.NewEventDto;
import ru.practicum.events.model.Location;
import ru.practicum.user_service.config.DateConfig;
import ru.practicum.events.model.Event;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.feign.UserClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EventMapper {
    private final CategoryDtoMapper categoryDtoMapper;
    private final UserClient userClient;

    public static NewEventDto toNewEventDto(Event event) {
        return NewEventDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(event.getCategory().getId())
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(DateConfig.FORMATTER))
                .location(new LocationDto(event.getLocation().getId(), event.getLocation().getLat(), event.getLocation().getLon()))
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .title(event.getTitle())
                .build();
    }

    public static Event dtoToEvent(NewEventDto dto, Long userId) {
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
                .location(new Location(dto.getLocation().getId(), dto.getLocation().getLat(), dto.getLocation().getLon()))
                .paid(dto.isPaid())
                .participantLimit(Objects.nonNull(dto.getParticipantLimit()) ? dto.getParticipantLimit() : 0)
                .requestModeration(Objects.nonNull(dto.getRequestModeration()) ? dto.getRequestModeration() : true)
                .initiatorId(userId)
                .createdOn(LocalDateTime.now())
                .publishedOn(LocalDateTime.now())
                .state(StateEvent.PENDING)
                .rating(0.0)
                .build();
    }

    public EventFullDto toEventFullDto(Event event) {
        String publishedOn = Objects.isNull(event.getPublishedOn()) ?
                null :
                event.getPublishedOn().format(DateConfig.FORMATTER);

        UserShortDto dto = userClient.getUser(event.getInitiatorId()).getBody();

        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDtoMapper.mapCategoryToDto(event.getCategory()))
                .confirmedRequests((Objects.isNull(event.getConfirmedRequests())) ? 0 : event.getConfirmedRequests())
                .eventDate(event.getEventDate().format(DateConfig.FORMATTER))
                .initiator(dto)
                .paid(event.isPaid())
                .title(event.getTitle())
                .rating((Objects.isNull(event.getRating())) ? 0.0 : event.getRating())
                .createdOn(event.getCreatedOn().format(DateConfig.FORMATTER))
                .description(event.getDescription())
                .location(new LocationDto(event.getLocation().getId(), event.getLocation().getLat(), event.getLocation().getLon()))
                .participantLimit(event.getParticipantLimit())
                .publishedOn(publishedOn)
                .requestModeration(event.isRequestModeration())
                .state(event.getState())
                .build();
    }

    public EventShortDto toEventShortDto(Event event) {
        UserShortDto dto = userClient.getUser(event.getInitiatorId()).getBody();

        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDtoMapper.mapCategoryToDto(event.getCategory()))
                .confirmedRequests((Objects.isNull(event.getConfirmedRequests())) ? 0 : event.getConfirmedRequests())
                .eventDate(event.getEventDate().format(DateConfig.FORMATTER))
                .initiator(dto)
                .paid(event.isPaid())
                .title(event.getTitle())
                .rating((Objects.isNull(event.getRating())) ? 0.0 : event.getRating())
                .build();
    }

    public List<EventShortDto> toListEventShortDto(List<Event> events) {
        return events.stream()
                .map(this::toEventShortDto)
                .toList();
    }

}