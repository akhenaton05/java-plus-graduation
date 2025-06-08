package ru.practicum.events.errors;

import org.springframework.dao.DataIntegrityViolationException;

public class NotPublishedEventParticipationException extends DataIntegrityViolationException {
    public NotPublishedEventParticipationException(String message) {
        super(message);
    }
}
