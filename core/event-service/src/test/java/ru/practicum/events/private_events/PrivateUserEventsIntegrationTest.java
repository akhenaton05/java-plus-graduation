package ru.practicum.events.private_events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.MainService;
import ru.practicum.category.model.Category;
import ru.practicum.category.service.CategoryServiceImpl;
import ru.practicum.event_service.dto.*;
import ru.practicum.events.model.Event;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.events.model.Location;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.user_service.dto.GetUserEventsDto;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.feign.UserClient;
import ru.practicum.events.service.PrivateUserEventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MainService.class, properties = "spring.profiles.active=test")
@ExtendWith(SpringExtension.class)
@Transactional
@Rollback(value = false)
public class PrivateUserEventsIntegrationTest {
    @Autowired
    private PrivateUserEventService privateUserEventService;
    @Autowired
    private CategoryServiceImpl categoryService;
    @Autowired
    private EventRepository eventRepository;
    @MockBean
    private UserClient userClient;

    private final LocationDto locationDto = new LocationDto(1L, 37, 56);
    private final NewEventDto eventDto = new NewEventDto(6L, "annotation", 1L, "descr", "2024-12-31 15:10:05", locationDto, true, 10, false, "Title");

    @BeforeEach
    void setUp() {
        // Mock UserClient
        when(userClient.getUser(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return ResponseEntity.ok(new UserShortDto(userId, "Test User " + userId));
        });
    }

    @Test
    void savingNewEvent() {
        EventFullDto fullEventDto = privateUserEventService.addNewEvent(1L, eventDto);

        assertAll(
                () -> assertEquals(eventDto.getTitle(), fullEventDto.getTitle()),
                () -> assertEquals(eventDto.getAnnotation(), fullEventDto.getAnnotation()),
                () -> assertEquals(eventDto.getCategory(), fullEventDto.getCategory().getId()),
                () -> assertEquals(eventDto.getDescription(), fullEventDto.getDescription()),
                () -> assertEquals(eventDto.getLocation(), fullEventDto.getLocation()),
                () -> assertEquals(eventDto.isPaid(), fullEventDto.isPaid()),
                () -> assertEquals(eventDto.getParticipantLimit(), fullEventDto.getParticipantLimit()),
                () -> assertEquals(eventDto.getRequestModeration(), fullEventDto.isRequestModeration()),
                () -> assertEquals(eventDto.getTitle(), fullEventDto.getTitle())
        );
    }

    @Test
    @Transactional
    void getUserEventById() {
        // Create and save a new Event
        Event event = new Event();
        event.setTitle("New title");
        event.setAnnotation("annotation");
        event.setDescription("description");
        event.setPaid(true);
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiatorId(1L);
        event.setLocation(new Location(null, 37, 56)); // New Location without ID
        event.setCategory(new Category(1L, "Category"));
        event.setEventDate(LocalDateTime.parse("2025-12-31T15:10:05"));
        event.setState(StateEvent.PENDING);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);

        // Save event to repository
        Event savedEvent = eventRepository.save(event);
        Long eventId = savedEvent.getId(); // Get the generated ID

        // Call service method
        EventFullDto fullEventDto = privateUserEventService.getUserEventById(1L, eventId);

        // Assertions
        assertNotNull(fullEventDto, "EventFullDto should not be null");
        assertAll(
                () -> assertEquals("New title", fullEventDto.getTitle()),
                () -> assertEquals(event.getAnnotation(), fullEventDto.getAnnotation()),
                () -> assertEquals(event.isPaid(), fullEventDto.isPaid())
        );
    }

    @Test
    void updatingEvent() {
        // Create and save an initial Event
        Event event = new Event();
        event.setTitle("Initial title");
        event.setAnnotation("annotation");
        event.setDescription("description");
        event.setPaid(true);
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiatorId(1L);
        event.setLocation(new Location(null, 37, 56)); // New Location without ID
        event.setCategory(new Category(1L, "Category"));
        event.setEventDate(LocalDateTime.parse("2025-12-31T15:10:05"));
        event.setState(StateEvent.CANCELED);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);

        Event savedEvent = eventRepository.save(event);
        Long eventId = savedEvent.getId();

        // Update event
        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest(
                1L, "annotationannotationannotation", 1, "descrdescrdescrdescrdescrdescrdescrdescrdescrdescrdescrdescrdescrdescrdescr",
                "2025-12-31 15:10:05", locationDto, true, 10, true, "CANCEL_REVIEW", "Title");

        EventFullDto updatedEvent = privateUserEventService.updateUserEvent(1L, eventId, updateRequest);

        assertAll(
                () -> assertEquals(updateRequest.getTitle(), updatedEvent.getTitle()),
                () -> assertEquals(updateRequest.getAnnotation(), updatedEvent.getAnnotation()),
                () -> assertEquals(updateRequest.getCategory(), updatedEvent.getCategory().getId()),
                () -> assertEquals(updateRequest.getDescription(), updatedEvent.getDescription()),
                () -> assertTrue(updatedEvent.isPaid()),
                () -> assertEquals(updateRequest.getParticipantLimit(), updatedEvent.getParticipantLimit()),
                () -> assertEquals(updateRequest.isRequestModeration(), updatedEvent.isRequestModeration()),
                () -> assertEquals(updateRequest.getTitle(), updatedEvent.getTitle())
        );
    }
}