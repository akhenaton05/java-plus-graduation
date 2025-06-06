package ru.practicum.request_service.errors;

import org.springframework.dao.DataIntegrityViolationException;

public class NotPublishedEventParticipationException extends DataIntegrityViolationException {
    public NotPublishedEventParticipationException(String message) {
        super(message);
    }
}
