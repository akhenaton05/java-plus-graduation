package ru.practicum.events.service;

import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.dto.NewEventDto;
import ru.practicum.event_service.dto.UpdateEventUserRequest;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.EventRequestStatusUpdateResult;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.user_service.dto.GetUserEventsDto;

import java.util.List;

public interface PrivateUserEventService {
    List<EventShortDto> getUsersEvents(GetUserEventsDto dto);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto addNewEvent(Long userId, NewEventDto eventDto);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateDto);

    List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateUserEventRequest(Long userId, Long eventId, EventRequestStatusUpdateRequest request);
}
