package ru.practicum.events.service;


import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.dto.SearchEventsParams;
import ru.practicum.events.model.Event;

import java.util.List;

public interface PublicEventsService {

    Event getEvent(Long id);

    EventFullDto getEventInfo(Long eventId, Long userId);

    List<EventShortDto> getFilteredEvents(SearchEventsParams searchEventsParams);

    EventFullDto getEventAnyStatusWithViews(Long id);

    EventFullDto getEventById(Long id);

    void likeEvent(Long userId, Long eventId);
}
