package ru.practicum.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.request_service.errors.EventOwnerParticipationException;
import ru.practicum.request_service.errors.EventParticipationLimitException;
import ru.practicum.request_service.errors.NotPublishedEventParticipationException;
import ru.practicum.request_service.errors.RepeatParticipationRequestException;


@Component
@RequiredArgsConstructor
public class ParticipationRequestValidator {

    private final ParticipationRequestRepository requestRepository;

    public RuntimeException checkRequest(Long userId, EventFullDto event, long confirmedRequestsCount) {
        if (event.getInitiator().getId().equals(userId)) {
            return new EventOwnerParticipationException("Event initiator cannot participate in their own event");
        }

        if (event.getState() != StateEvent.PUBLISHED) {
            return new NotPublishedEventParticipationException("Cannot participate in an unpublished event");
        }

        if (requestRepository.existsByUserIdAndEventId(userId, event.getId())) {
            return new RepeatParticipationRequestException("User already has a participation request for this event");
        }

        if (event.getParticipantLimit() > 0 && confirmedRequestsCount >= event.getParticipantLimit()) {
            return new EventParticipationLimitException("Event participant limit reached");
        }

        return null;
    }
}