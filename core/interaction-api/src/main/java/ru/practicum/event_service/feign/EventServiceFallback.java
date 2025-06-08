package ru.practicum.event_service.feign;

import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.user_service.exceptions.ServiceUnavailableException;


@Component
public class EventServiceFallback {

    @GetMapping("/by-id/{id}")
    ResponseEntity<EventFullDto> getEventById(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        throw new ServiceUnavailableException("EventService unavailable");
    }

    @GetMapping("/get-event-status/{id}")
    ResponseEntity<EventFullDto> getEventAnyStatusWithViews(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        throw new ServiceUnavailableException("EventService unavailable");
    }
}
