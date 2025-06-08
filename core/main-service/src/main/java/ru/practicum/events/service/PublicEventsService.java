package ru.practicum.events.service;


import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.dto.LookEventDto;
import ru.practicum.event_service.dto.SearchEventsParams;
import ru.practicum.events.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicEventsService {

    Event getEvent(Long id);

    int getEventsViews(long id, LocalDateTime eventDate);

    EventFullDto getEventInfo(LookEventDto lookEventDto);

    List<EventShortDto> getFilteredEvents(SearchEventsParams searchEventsParams, LookEventDto lookEventDto);

    EventFullDto getEventAnyStatusWithViews(Long id);

    EventFullDto getEventById(Long id);
}
