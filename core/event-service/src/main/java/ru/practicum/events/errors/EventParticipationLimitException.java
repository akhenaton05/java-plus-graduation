package ru.practicum.events.errors;

import org.springframework.dao.DataIntegrityViolationException;

public class EventParticipationLimitException extends DataIntegrityViolationException {
    public EventParticipationLimitException(String message) {
        super(message);
    }
}
