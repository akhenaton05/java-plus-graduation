package ru.practicum.errors;

import lombok.Data;
import ru.practicum.user_service.config.DateConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ApiError {
    private List<String> errors;
    private String message;
    private String reason;
    private String status;
    private String timestamp;

    public ApiError(String status, String reason, Throwable ex) {
        this.errors = acceptStackTrace(ex);
        this.message = ex.getMessage();
        this.reason = reason;
        this.status = status;
        this.timestamp = LocalDateTime.now().format(DateConfig.FORMATTER);
    }

    private List<String> acceptStackTrace(Throwable ex) {
        return List.of(ex.getStackTrace()).stream()
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
    }
}
