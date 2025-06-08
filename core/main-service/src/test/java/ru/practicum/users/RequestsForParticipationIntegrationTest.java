package ru.practicum.users;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

//@SpringBootTest(classes = RequestService.class, properties = "spring.profiles.active=test")
//@ExtendWith(SpringExtension.class)
//@Transactional(readOnly = true)
//@Slf4j
//@RequiredArgsConstructor
//public class RequestsForParticipationIntegrationTest {
//    @Autowired
//    private ParticipationRequestService participationRequestService;
//    @Autowired
//    private PrivateUserEventService eventService;
//    @Autowired
//    private CategoryService categoryService;
//    @Autowired
//    private AdminEventService adminEventService;
//    @MockBean
//    private UserClient userClient; // Мок для UserClient
//
//    private UserDto eventOwner;
//    private UserDto eventParticipant;
//    private UserDto eventSecondParticipant;
//    private EventFullDto pendingEvent;
//
//    @BeforeEach
//    void setUp() {
//        // Мокаем создание пользователей через UserClient
//        eventOwner = new UserDto(1L, "Test User", "eventOwner@example.com");
//        when(userClient.addUser(new NewUserRequest("Test User", "eventOwner@example.com")))
//                .thenReturn(ResponseEntity.ok(eventOwner));
//        when(userClient.getUser(1L)).thenReturn(ResponseEntity.ok(new UserShortDto(1L, "Test User")));
//
//        eventParticipant = new UserDto(2L, "Test User", "eventParticipant@example.com");
//        when(userClient.addUser(new NewUserRequest("Test User", "eventParticipant@example.com")))
//                .thenReturn(ResponseEntity.ok(eventParticipant));
//        when(userClient.getUser(2L)).thenReturn(ResponseEntity.ok(new UserShortDto(2L, "Test User")));
//
//        eventSecondParticipant = new UserDto(3L, "Test User", "eventSecondParticipant@example.com");
//        when(userClient.addUser(new NewUserRequest("Test User", "eventSecondParticipant@example.com")))
//                .thenReturn(ResponseEntity.ok(eventSecondParticipant));
//        when(userClient.getUser(3L)).thenReturn(ResponseEntity.ok(new UserShortDto(3L, "Test User")));
//
//        // Обобщённый мок для любых других ID пользователей
//        when(userClient.getUser(anyLong())).thenAnswer(invocation -> {
//            Long userId = invocation.getArgument(0);
//            return ResponseEntity.ok(new UserShortDto(userId, "Test User " + userId));
//        });
//
//        // Создание категории
//        NewCategoryDto newCategory = new NewCategoryDto();
//        newCategory.setName("Test Category");
//        CategoryDto category = categoryService.addCategory(newCategory);
//
//        // Создание события
//        LocationDto location = new LocationDto();
//        location.setLat(0.12f);
//        location.setLon(0.11f);
//
//        NewEventDto newEventDto = new NewEventDto();
//        newEventDto.setTitle("Test Event");
//        newEventDto.setDescription("Description");
//        newEventDto.setAnnotation("some annotation");
//        newEventDto.setCategory(Math.toIntExact(category.getId()));
//        newEventDto.setParticipantLimit(1);
//        newEventDto.setEventDate("9999-02-02 12:12:12");
//        newEventDto.setRequestModeration(false);
//        newEventDto.setPaid(false);
//        newEventDto.setLocation(location);
//
//        // Мокаем вызовы UserClient в EventMapper
//        when(userClient.getUser(eventOwner.getId())).thenReturn(ResponseEntity.ok(new UserShortDto(eventOwner.getId(), eventOwner.getName())));
//
//        pendingEvent = eventService.addNewEvent(eventOwner.getId(), newEventDto);
//    }
//
//    @Test
//    void getUserRequests_ShouldReturnEmptyListInitially() {
//        List<ParticipationRequestDto> requests = participationRequestService.getUserRequests(eventOwner.getId());
//        AssertionsForInterfaceTypes.assertThat(requests).isEmpty();
//    }
//
//    @Test
//    @Transactional
//    void addParticipationRequest_ShouldCreateNewRequest() {
//        UpdateEventAdminRequest updateEvent = new UpdateEventAdminRequest();
//        updateEvent.setStateAction(EventStateAction.PUBLISH_EVENT);
//        EventFullDto eventFullDto = adminEventService.updateEvent(pendingEvent.getId(), updateEvent);
//
//        ParticipationRequestDto requestDto = participationRequestService
//                .addParticipationRequest(eventParticipant.getId(), eventFullDto.getId());
//
//        AssertionsForClassTypes.assertThat(requestDto).isNotNull();
//        AssertionsForClassTypes.assertThat(requestDto.getRequester()).isEqualTo(eventParticipant.getId());
//        AssertionsForClassTypes.assertThat(requestDto.getEvent()).isEqualTo(pendingEvent.getId());
//        AssertionsForInterfaceTypes.assertThat(requestDto.getStatus()).isEqualTo(ParticipationRequestStatus.CONFIRMED);
//    }
//
//    @Test
//    @Transactional
//    void addParticipationRequest_ShouldThrowError() {
//        assertThrows(EventOwnerParticipationException.class, () -> {
//            participationRequestService.addParticipationRequest(eventOwner.getId(), pendingEvent.getId());
//        });
//
//        assertThrows(NotPublishedEventParticipationException.class, () -> {
//            participationRequestService.addParticipationRequest(eventParticipant.getId(), pendingEvent.getId());
//        });
//
//        UpdateEventAdminRequest updateEvent = new UpdateEventAdminRequest();
//        updateEvent.setStateAction(EventStateAction.PUBLISH_EVENT);
//        EventFullDto eventFullDto = adminEventService.updateEvent(pendingEvent.getId(), updateEvent);
//
//        // Сначала добавляем запрос от eventParticipant
//        participationRequestService.addParticipationRequest(eventParticipant.getId(), eventFullDto.getId());
//
//        // Проверяем лимит участников
//        assertThrows(EventParticipationLimitException.class, () -> {
//            participationRequestService.addParticipationRequest(eventSecondParticipant.getId(), eventFullDto.getId());
//        });
//
//        // Проверяем повторный запрос от eventParticipant
//        assertThrows(RepeatParticipationRequestException.class, () -> {
//            participationRequestService.addParticipationRequest(eventParticipant.getId(), eventFullDto.getId());
//        });
//    }
//
//    @Test
//    @Transactional
//    void cancelRequest_ShouldChangeRequestStatusToCanceled() {
//        UpdateEventAdminRequest updateEvent = new UpdateEventAdminRequest();
//        updateEvent.setStateAction(EventStateAction.PUBLISH_EVENT);
//        EventFullDto eventFullDto = adminEventService.updateEvent(pendingEvent.getId(), updateEvent);
//
//        ParticipationRequestDto requestDto = participationRequestService
//                .addParticipationRequest(eventParticipant.getId(), eventFullDto.getId());
//        ParticipationRequestDto canceledRequest = participationRequestService
//                .cancelRequest(eventParticipant.getId(), requestDto.getId());
//
//        AssertionsForInterfaceTypes.assertThat(canceledRequest.getStatus()).isEqualTo(ParticipationRequestStatus.CANCELED);
//    }
//}