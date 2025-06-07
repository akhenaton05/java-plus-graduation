package ru.practicum.users.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.events.model.Event;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.request_service.feign.RequestClient;
import ru.practicum.users.errors.EventOwnerParticipationException;
import ru.practicum.users.errors.EventParticipationLimitException;
import ru.practicum.users.errors.NotPublishedEventParticipationException;
import ru.practicum.users.errors.RepeatParticipationRequestException;

@Component
@RequiredArgsConstructor
public class ParticipationRequestValidator {

//    private final ParticipationRequestRepository requestRepository;
    private final RequestClient requestService;

    public RuntimeException checkRequest(Long userId, Event event, long confirmedRequestsCount) {
        if (event.getInitiatorId().equals(userId)) {
            return new EventOwnerParticipationException("Event initiator cannot participate in their own event");
        }

        if (event.getState() != StateEvent.PUBLISHED) {
            return new NotPublishedEventParticipationException("Cannot participate in an unpublished event");
        }

        if (requestService.existsByUserIdAndEventId(userId, event.getId())) {
            return new RepeatParticipationRequestException("User already has a participation request for this event");
        }

        if (event.getParticipantLimit() > 0 && confirmedRequestsCount >= event.getParticipantLimit()) {
            return new EventParticipationLimitException("Event participant limit reached");
        }

        return null;
    }
}