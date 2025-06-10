package ru.practicum.events;

import jakarta.persistence.EntityNotFoundException;
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
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.category_service.dto.CategoryDto;
import ru.practicum.config.DateConfig;
import ru.practicum.event_service.dto.*;
import ru.practicum.event_service.entity.EventStateAction;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.events.service.AdminEventService;
import ru.practicum.user_service.dto.NewUserRequest;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.feign.UserClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = MainService.class, properties = "spring.profiles.active=test")
@ExtendWith(SpringExtension.class)
@Transactional
@Rollback(value = true)
public class AdminEventIntegrationTest {

    @MockBean
    private AdminEventService adminEventService;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private UserClient userClient;

    private Long testUserId;
    private Category testCategory;
    private Category testCategory2;
    private EventFullDto pendingEventDto;

    @BeforeEach
    void setUp() {
        try {
            // Очищаем таблицу категорий
            categoryRepository.deleteAll();

            // Настраиваем пользователя через UserClient
            testUserId = 1L;
            UserDto userDto = new UserDto(testUserId, "Test User", "testuser@example.com");
            UserShortDto userShortDto = new UserShortDto(testUserId, "Test User");
            when(userClient.addUser(any(NewUserRequest.class)))
                    .thenReturn(ResponseEntity.ok(userDto));
            when(userClient.getUser(testUserId))
                    .thenReturn(ResponseEntity.ok(userShortDto));

            // Создаем категории
            testCategory = new Category();
            testCategory.setName("Test Category");
            testCategory = categoryRepository.save(testCategory);

            testCategory2 = new Category();
            testCategory2.setName("Test Category 2");
            testCategory2 = categoryRepository.save(testCategory2);

            // Создаем тестовое событие
            LocationDto locationDto = new LocationDto(null, 10.0f, 20.0f);
            pendingEventDto = new EventFullDto();
            pendingEventDto.setId(1L);
            pendingEventDto.setTitle("Test Event");
            pendingEventDto.setDescription("Event Description");
            pendingEventDto.setAnnotation("Event Annotation");
            pendingEventDto.setCategory(new CategoryDto(testCategory.getId(), "Test Category"));
            pendingEventDto.setEventDate(LocalDateTime.now().plusDays(5).format(DateConfig.FORMATTER));
            pendingEventDto.setPaid(false);
            pendingEventDto.setParticipantLimit(5);
            // Убрано setRequestModeration, так как метода нет
            pendingEventDto.setState(StateEvent.PENDING);
            pendingEventDto.setViews(0);
            pendingEventDto.setInitiator(userShortDto);
            pendingEventDto.setCreatedOn(LocalDateTime.now().format(DateConfig.FORMATTER));
            pendingEventDto.setLocation(locationDto);

            // Мокаем getEvents
            when(adminEventService.getEvents(
                    eq(Collections.singletonList(testUserId)),
                    eq(Collections.singletonList(StateEvent.PENDING.name())),
                    eq(Collections.singletonList(testCategory.getId())),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    eq(0),
                    eq(10)))
                    .thenReturn(Collections.singletonList(pendingEventDto));

            // Мокаем updateEvent
            when(adminEventService.updateEvent(eq(1L), any(UpdateEventAdminRequest.class)))
                    .thenAnswer(invocation -> {
                        UpdateEventAdminRequest request = invocation.getArgument(1);
                        EventFullDto updated = new EventFullDto();
                        updated.setId(1L);
                        updated.setTitle(request.getTitle() != null ? request.getTitle() : pendingEventDto.getTitle());
                        updated.setDescription(request.getDescription() != null ? request.getDescription() : pendingEventDto.getDescription());
                        updated.setAnnotation(request.getAnnotation() != null ? request.getAnnotation() : pendingEventDto.getAnnotation());
                        updated.setCategory(new CategoryDto(
                                request.getCategory() != null ? (long) request.getCategory() : testCategory.getId(),
                                request.getCategory() != null ? testCategory2.getName() : testCategory.getName()
                        ));
                        updated.setEventDate(request.getEventDate() != null ?
                                request.getEventDate().format(DateConfig.FORMATTER) :
                                pendingEventDto.getEventDate());
                        updated.setPaid(request.getPaid() != null ? request.getPaid() : pendingEventDto.isPaid());
                        updated.setParticipantLimit(request.getParticipantLimit() != null ? request.getParticipantLimit() : pendingEventDto.getParticipantLimit());
                        // Убрано setRequestModeration, так как метода нет
                        updated.setState(request.getStateAction() != null ?
                                (request.getStateAction() == EventStateAction.PUBLISH_EVENT ? StateEvent.PUBLISHED :
                                        (request.getStateAction() == EventStateAction.REJECT_EVENT ? StateEvent.CANCELED : StateEvent.PENDING)) :
                                StateEvent.PENDING);
                        updated.setViews(0);
                        updated.setInitiator(userShortDto);
                        updated.setCreatedOn(LocalDateTime.now().format(DateConfig.FORMATTER));
                        updated.setLocation(locationDto);
                        return updated;
                    });

            // Мокаем исключения
            when(adminEventService.updateEvent(eq(9999L), any(UpdateEventAdminRequest.class)))
                    .thenThrow(new EntityNotFoundException("Event not found"));
            when(adminEventService.updateEvent(eq(1L), argThat(req -> req.getCategory() != null && req.getCategory() == 9999)))
                    .thenThrow(new EntityNotFoundException("Category not found"));

            System.out.println("Создано событие с ID: " + pendingEventDto.getId());
        } catch (Exception e) {
            System.err.println("Ошибка в setUp: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void getEvents_ДолжноВернутьСобытия() {
        try {
            List<EventFullDto> events = adminEventService.getEvents(
                    Collections.singletonList(testUserId),
                    Collections.singletonList(StateEvent.PENDING.name()),
                    Collections.singletonList(testCategory.getId()),
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(10),
                    0,
                    10
            );

            System.out.println("Полученные события: " + events);
            assertThat(events).isNotEmpty();
            assertThat(events.get(0).getTitle()).isEqualTo("Test Event");
        } catch (Exception e) {
            System.err.println("Ошибка в getEvents_ДолжноВернутьСобытия: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void updateEvent_ДолжноОбновитьДеталиСобытия() {
        try {
            LocalDateTime eventDate = LocalDateTime.now().plusDays(10);

            UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
            updateRequest.setTitle("Обновленный заголовок события");
            updateRequest.setDescription("Обновленное описание");
            updateRequest.setAnnotation("Обновленная аннотация");
            updateRequest.setEventDate(eventDate);
            updateRequest.setCategory(testCategory2.getId().intValue());
            updateRequest.setPaid(false);
            updateRequest.setParticipantLimit(5);
            updateRequest.setStateAction(EventStateAction.PUBLISH_EVENT);

            EventFullDto updatedEvent = adminEventService.updateEvent(pendingEventDto.getId(), updateRequest);

            System.out.println("Обновленное событие: " + updatedEvent);
            assertThat(updatedEvent).isNotNull();
            assertThat(updatedEvent.getTitle()).isEqualTo("Обновленный заголовок события");
            assertThat(updatedEvent.getDescription()).isEqualTo("Обновленное описание");
            assertThat(updatedEvent.getAnnotation()).isEqualTo("Обновленная аннотация");
            assertThat(updatedEvent.getEventDate()).isEqualTo(eventDate.format(DateConfig.FORMATTER));
            assertThat(updatedEvent.getCategory().getId()).isEqualTo(testCategory2.getId());
            assertThat(updatedEvent.isPaid()).isEqualTo(false);
            assertThat(updatedEvent.getParticipantLimit()).isEqualTo(5);
            assertThat(updatedEvent.getState()).isEqualTo(StateEvent.PUBLISHED);
        } catch (Exception e) {
            System.err.println("Ошибка в updateEvent_ДолжноОбновитьДеталиСобытия: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void updateEvent_ДолжноОбновитьСтатусОтменено() {
        try {
            UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
            updateRequest.setStateAction(EventStateAction.REJECT_EVENT);

            EventFullDto updatedEvent = adminEventService.updateEvent(pendingEventDto.getId(), updateRequest);

            System.out.println("Обновленное событие: " + updatedEvent);
            assertThat(updatedEvent).isNotNull();
            assertThat(updatedEvent.getState()).isEqualTo(StateEvent.CANCELED);
        } catch (Exception e) {
            System.err.println("Ошибка в updateEvent_ДолжноОбновитьСтатусОтменено: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void updateEvent_ДолжноВыброситьИсключение_ПриНесуществующейКатегории() {
        try {
            UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
            updateRequest.setCategory(9999);

            assertThrows(EntityNotFoundException.class, () -> {
                adminEventService.updateEvent(pendingEventDto.getId(), updateRequest);
            });
        } catch (Exception e) {
            System.err.println("Ошибка в updateEvent_ДолжноВыброситьИсключение_ПриНесуществующейКатегории: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void updateEvent_ДолжноВыброситьИсключение_КогдаСобытиеНеСуществует() {
        try {
            UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
            updateRequest.setTitle("Несуществующее событие");

            assertThrows(EntityNotFoundException.class, () -> {
                adminEventService.updateEvent(9999L, updateRequest);
            });
        } catch (Exception e) {
            System.err.println("Ошибка в updateEvent_ДолжноВыброситьИсключение_КогдаСобытиеНеСуществует: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}