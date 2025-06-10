package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.dto.LookEventDto;
import ru.practicum.event_service.dto.SearchEventsParams;
import ru.practicum.events.service.PublicEventsService;
import ru.practicum.event_service.validation.SearchParamsValidator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicEventController {

    private final PublicEventsService publicEventsService;
    private final SearchParamsValidator searchParamsValidator;

    @GetMapping
    public ResponseEntity<List<EventShortDto>>
    getFilteredEvents(@RequestParam(required = false, defaultValue = "") String text,
                      @RequestParam(required = false, defaultValue = "") List<Long> categories,
                      @RequestParam(required = false) Boolean paid,
                      @RequestParam(required = false) String rangeStart,
                      @RequestParam(required = false) String rangeEnd,
                      @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
                      @RequestParam(required = false, defaultValue = "EVENT_DATE") String sort,
                      @RequestParam(required = false, defaultValue = "0") int from,
                      @RequestParam(required = false, defaultValue = "10") int size,
                      HttpServletRequest request) {
        String encodedUri = URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8);
        LookEventDto lookEventDto = LookEventDto.builder()
                .id(null)
                .uri(encodedUri)
                .ip(request.getRemoteAddr())
                .build();
        SearchEventsParams searchEventsParams =
                new SearchEventsParams(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        log.info("\nPublicEventController.getFilteredEvents {}", searchEventsParams);

        SearchParamsValidator.validateSearchParams(searchEventsParams);
        List<EventShortDto> result = publicEventsService.getFilteredEvents(searchEventsParams, lookEventDto);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEventInfo(@PathVariable
                                                     @Min(value = 1, message = "ID must be positive") Long id,
                                                     HttpServletRequest request) {
        String encodedUri = request.getRequestURI();
        LookEventDto lookEventDto = LookEventDto.builder()
                .id(id)
                .uri(encodedUri)
                .ip(request.getRemoteAddr())
                .build();
        log.info("\nPublicEventController.getEventInfo accepted {}", lookEventDto);

        EventFullDto eventFullDto = publicEventsService.getEventInfo(lookEventDto);
        return ResponseEntity.status(HttpStatus.OK).body(eventFullDto);
    }

    @GetMapping("/by-id/{id}")
    //Метод для Feign клиента, без регистрации просмотра пользователем
    public ResponseEntity<EventFullDto> getEventById(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        log.info("\nNew get event request for id {}", id);
        EventFullDto eventFullDto = publicEventsService.getEventById(id);
        return ResponseEntity.status(HttpStatus.OK).body(eventFullDto);
    }

    @GetMapping("/get-event-status/{id}")
    //Метод для Feign клиента, без регистрации просмотра пользователем
    public ResponseEntity<EventFullDto> getEventAnyStatusWithViews(@PathVariable @Min(value = 1, message = "ID must be positive") Long id) {
        log.info("\nNew get event request for getting status for id {}", id);
        EventFullDto eventFullDto = publicEventsService.getEventAnyStatusWithViews(id);
        return ResponseEntity.status(HttpStatus.OK).body(eventFullDto);
    }
}