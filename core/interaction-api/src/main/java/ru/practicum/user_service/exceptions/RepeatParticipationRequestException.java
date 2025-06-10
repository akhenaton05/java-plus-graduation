package ru.practicum.user_service.exceptions;

import org.springframework.dao.DataIntegrityViolationException;

public class RepeatParticipationRequestException extends DataIntegrityViolationException {
    public RepeatParticipationRequestException(String message) {
        super(message);
    }
}
