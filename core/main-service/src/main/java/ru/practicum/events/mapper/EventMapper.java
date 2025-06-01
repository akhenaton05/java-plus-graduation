package ru.practicum.events.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.mapstruct.IterableMapping;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.mapper.CategoryDtoMapper;
import ru.practicum.category.model.Category;
import ru.practicum.config.DateConfig;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.NewEventDto;
import ru.practicum.events.model.Event;
import ru.practicum.users.dto.UserShortDto;
import ru.practicum.users.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring",
        uses = {CategoryDtoMapper.class},
        imports = {DateConfig.class, LocalDateTime.class, Category.class})
public interface EventMapper {

    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    @Mapping(target = "category", source = "category.id")
    @Mapping(target = "eventDate", expression = "java(event.getEventDate().format(DateConfig.FORMATTER))")
    NewEventDto toNewEventDto(Event event);

    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "category", expression = "java(Category.builder().id(dto.getCategory()).build())")
    @Mapping(target = "eventDate", expression = "java(LocalDateTime.parse(dto.getEventDate(), DateConfig.FORMATTER))")
    @Mapping(target = "participantLimit", expression = "java(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)")
    @Mapping(target = "requestModeration", expression = "java(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)")
    @Mapping(target = "initiator", source = "user")
    @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())")
    @Mapping(target = "publishedOn", expression = "java(LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "views", constant = "0")
    Event toEvent(NewEventDto dto, User user);

    @Mapping(target = "category", source = "category")
    @Mapping(target = "confirmedRequests", expression = "java(event.getConfirmedRequests() == null ? 0 : event.getConfirmedRequests())")
    @Mapping(target = "eventDate", expression = "java(event.getEventDate().format(DateConfig.FORMATTER))")
    @Mapping(target = "initiator", expression = "java(new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()))")
    @Mapping(target = "views", expression = "java(event.getViews() == null ? 0 : event.getViews())")
    @Mapping(target = "createdOn", expression = "java(event.getCreatedOn().format(DateConfig.FORMATTER))")
    @Mapping(target = "publishedOn", expression = "java(event.getPublishedOn() == null ? null : event.getPublishedOn().format(DateConfig.FORMATTER))")
    EventFullDto toEventFullDto(Event event);

    @Mapping(target = "category", source = "category")
    @Mapping(target = "confirmedRequests", expression = "java(event.getConfirmedRequests() == null ? 0 : event.getConfirmedRequests())")
    @Mapping(target = "eventDate", expression = "java(event.getEventDate().format(DateConfig.FORMATTER))")
    @Mapping(target = "initiator", expression = "java(new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()))")
    @Mapping(target = "views", expression = "java(event.getViews() == null ? 0 : event.getViews())")
    EventShortDto toEventShortDto(Event event);

    @IterableMapping(qualifiedByName = "toEventShortDto")
    List<EventShortDto> toListEventShortDto(List<Event> events);

    @Named("toEventShortDto")
    EventShortDto toEventShortDtoNamed(Event event);
}