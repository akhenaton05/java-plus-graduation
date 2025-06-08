package ru.practicum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.event_service.feign.EventClient;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.request_service.entity.ParticipationRequestStatus;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.feign.UserClient;
import ru.practicum.validation.ParticipationRequestValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final ParticipationRequestValidator participationRequestValidator;
    private final ParticipationRequestMapper participationRequestMapper;
    private final EventClient eventClient;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userClient.getUser(userId);
        return requestRepository.findByUserId(userId)
                .stream()
                .map(participationRequestMapper::mapToDto)
                .toList();
    }

    public int getConfirmedRequests(long eventId) {
        return requestRepository
                .countConfirmedRequestsByStatusAndEventId(ParticipationRequestStatus.CONFIRMED, eventId);
    }

    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        UserShortDto user = userClient.getUser(userId).getBody();
        EventFullDto event = getEvent(eventId);

        long confirmedRequestsCount = getConfirmedRequests(eventId);

        RuntimeException validationError =
                participationRequestValidator.checkRequest(user.getId(), event, confirmedRequestsCount);

        if (Objects.nonNull(validationError))
            throw validationError;

        ParticipationRequest request = new ParticipationRequest();
        request.setUserId(user.getId());
        request.setEventId(eventId);
        if (event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
        } else {
            request.setStatus(event.isRequestModeration() ? ParticipationRequestStatus.PENDING : ParticipationRequestStatus.CONFIRMED);
        }
        request.setCreated(LocalDateTime.now());


        ParticipationRequest savedRequest = requestRepository.save(request);
        return participationRequestMapper.mapToDto(savedRequest);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Request with id=" + requestId + " was not found"));

        request.setStatus(ParticipationRequestStatus.CANCELED);
        requestRepository.save(request);

        return participationRequestMapper.mapToDto(request);
    }

    private EventFullDto getEvent(Long eventId) {
        return eventClient.getEventById(eventId).getBody();
    }

    public List<ParticipationRequestDto> findRequestsByEventId(Long eventId) {
         return requestRepository.findByEventId(eventId).stream()
                 .map(participationRequestMapper::mapToDto)
                 .toList();
    }

    public List<ParticipationRequestDto> findByRequestIds(List<Long> requestIds) {
        return requestRepository.findByIds(requestIds).stream()
                .map(participationRequestMapper::mapToDto)
                .toList();
    }

    @Transactional
    public void updateStatusByIds(EventRequestStatusUpdateRequest request) {
        requestRepository.updateStatusByIds(ParticipationRequestStatus.valueOf(request.getStatus()), request.getRequestIds());
    }

    public boolean existsByUserIdAndEventId(Long userId, Long eventId) {
        return requestRepository.existsByUserIdAndEventId(userId, eventId);
    }

    public Integer getConfirmedRequestsCount(Long eventId) {
        return requestRepository.countConfirmedRequestsByStatusAndEventId(ParticipationRequestStatus.CONFIRMED, eventId);
    }
}