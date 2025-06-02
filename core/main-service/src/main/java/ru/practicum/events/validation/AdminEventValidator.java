package ru.practicum.events.validation;

import ru.practicum.errors.ForbiddenActionException;
import ru.practicum.events.dto.UpdateEventAdminRequest;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventStateAction;
import ru.practicum.events.model.StateEvent;

import java.time.LocalDateTime;
import java.util.Objects;

public class AdminEventValidator {
    public static void validateEventStatusUpdate(Event event, UpdateEventAdminRequest updateRequest) {
        LocalDateTime eventDate = updateRequest.getEventDate();

        if (Objects.nonNull(updateRequest.getEventDate()) && eventDate.isBefore(event.getCreatedOn().plusHours(1))) {
            throw new ForbiddenActionException("Event start time must be at least 1 hour after publication.");
        }

        if (Objects.isNull(updateRequest.getStateAction()))
            return;

        boolean isPublishAction = updateRequest.getStateAction()
                .equals(EventStateAction.PUBLISH_EVENT);
        boolean isRejectAction = updateRequest.getStateAction()
                .equals(EventStateAction.REJECT_EVENT);

        if (isPublishAction && !event.getState().equals(StateEvent.PENDING)) {
            throw new ForbiddenActionException("Cannot publish event. It must be in PENDING state.");
        }

        if (isRejectAction && event.getState().equals(StateEvent.PUBLISHED)) {
            throw new ForbiddenActionException("Cannot reject event. It is already published.");
        }
    }
}
