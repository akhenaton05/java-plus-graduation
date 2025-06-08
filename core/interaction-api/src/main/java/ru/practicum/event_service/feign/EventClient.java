package ru.practicum.event_service.feign;

import jakarta.validation.constraints.Min;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.event_service.dto.EventFullDto;

@FeignClient(name = "event-service", path = "/events")
public interface EventClient {

    @GetMapping("/by-id/{id}")
    //Метод для Feign клиента, без регистрации просмотра пользователем
    ResponseEntity<EventFullDto> getEventById(@PathVariable @Min(value = 1, message = "ID must be positive") Long id);

    @GetMapping("/get-event-status/{id}")
    ResponseEntity<EventFullDto> getEventAnyStatusWithViews(@PathVariable @Min(value = 1, message = "ID must be positive") Long id);
}
