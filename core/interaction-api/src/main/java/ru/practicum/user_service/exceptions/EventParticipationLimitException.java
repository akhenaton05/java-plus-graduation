package ru.practicum.user_service.exceptions;

import org.springframework.dao.DataIntegrityViolationException;

public class EventParticipationLimitException extends DataIntegrityViolationException {
    public EventParticipationLimitException(String message) {
        super(message);
    }
}
