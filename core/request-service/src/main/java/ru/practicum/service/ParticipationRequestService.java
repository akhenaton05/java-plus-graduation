package ru.practicum.service;

import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getUserRequests(Long userId);

    int getConfirmedRequests(long eventId);

    ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    EventFullDto getEvent(Long eventId);

    List<ParticipationRequestDto> findRequestsByEventId(Long eventId);

    List<ParticipationRequestDto> findByRequestIds(List<Long> requestIds);

    void updateStatusByIds(EventRequestStatusUpdateRequest request);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Integer getConfirmedRequestsCount(Long eventId);
}
