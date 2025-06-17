package ru.practicum.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.MainService;
import ru.practicum.category_service.dto.CategoryDto;
import ru.practicum.event_service.dto.*;
import ru.practicum.user_service.config.StatsClientConfig;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.service.PublicEventsService;
import ru.practicum.user_service.dto.UserShortDto;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MainService.class)
@AutoConfigureMockMvc
@RequiredArgsConstructor
public class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "publicEventsService")
    private PublicEventsService service;

    @MockBean
    private StatsClientConfig statsClientConfig;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private EventRepository eventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String firstDate = "2024-12-10 14:30:00";
    private final String secondDate = "2025-03-10 14:30:00";
    private final String thirdDate = "2024-12-11 14:30:00";

    private final EventFullDto eventFullDto = EventFullDto.builder()
            .id(1L)
            .annotation("12345".repeat(5))
            .category(new CategoryDto())
            .eventDate(firstDate)
            .confirmedRequests(0)
            .paid(true)
            .title("without")
            .initiator(new UserShortDto())
            .rating(4.5) // Updated to include rating
            .createdOn(secondDate)
            .description("12345".repeat(6))
            .publishedOn(thirdDate)
            .location(new LocationDto())
            .participantLimit(0)
            .requestModeration(true)
            .state(StateEvent.PUBLISHED)
            .build();

    private final EventShortDto eventShortDto = EventShortDto.builder()
            .id(1L)
            .annotation("12345".repeat(5))
            .category(new CategoryDto())
            .confirmedRequests(0)
            .eventDate(firstDate)
            .initiator(new UserShortDto())
            .paid(true)
            .title("First")
            .rating(4.5) // Updated to include rating
            .build();

    @Test
    @SneakyThrows
    public void getEventInfo_whenValidParams_thenGetResponse() {
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(List.of(mockInstance));

        when(service.getEventInfo(anyLong(), anyLong())).thenReturn(eventFullDto);
        long eventId = 1L;
        long userId = 1L;

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/events/{id}", eventId)
                        .header("X-EWM-USER-ID", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(mvcResult.getResponse());
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        EventFullDto actualEvent = objectMapper.readValue(jsonResponse, EventFullDto.class);

        ArgumentCaptor<Long> eventIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(service).getEventInfo(eventIdCaptor.capture(), userIdCaptor.capture());

        assertEquals(eventId, eventIdCaptor.getValue());
        assertEquals(userId, userIdCaptor.getValue());
        assertEquals(eventFullDto.getId(), actualEvent.getId());
        assertEquals(eventFullDto.getRating(), actualEvent.getRating());
    }

    @Test
    @SneakyThrows
    public void getFilteredEvents_whenCallMethod_thenGetResponse() {
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(List.of(mockInstance));

        List<EventShortDto> expectedList = List.of(eventShortDto);
        when(service.getFilteredEvents(any())).thenReturn(expectedList);

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/events")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("rangeStart", thirdDate)
                        .param("rangeEnd", secondDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(mvcResult.getResponse());
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        List<EventShortDto> factList = objectMapper.readValue(jsonResponse, new TypeReference<>() {});
        assertEquals(1, factList.size());
        assertEquals(eventShortDto.getId(), factList.getFirst().getId());
        assertEquals(eventShortDto.getRating(), factList.getFirst().getRating());

        ArgumentCaptor<SearchEventsParams> searchParamCaptor = ArgumentCaptor.forClass(SearchEventsParams.class);
        verify(service).getFilteredEvents(searchParamCaptor.capture());
        SearchEventsParams capturedParams = searchParamCaptor.getValue();

        assertEquals("", capturedParams.getText(), "text");
        assertTrue(capturedParams.getCategories().isEmpty(), "categories");
        assertNull(capturedParams.getPaid(), "paid");
        assertEquals(thirdDate, capturedParams.getRangeStart(), "rangeStart");
        assertEquals(secondDate, capturedParams.getRangeEnd(), "rangeEnd");
        assertFalse(capturedParams.getOnlyAvailable(), "onlyAvailable");
        assertEquals("EVENT_DATE", capturedParams.getSort(), "sort");
        assertEquals(0, capturedParams.getFrom(), "from");
        assertEquals(10, capturedParams.getSize(), "size");
    }

    @Test
    @SneakyThrows
    public void getFilteredEvents_whenValidParams_thenReturnCorrectEventDateFormat() {
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(Arrays.asList(mockInstance));

        EventShortDto eventDto = EventShortDto.builder()
                .id(1L)
                .annotation("Test event")
                .category(new CategoryDto(1L, "Test Category"))
                .confirmedRequests(0)
                .eventDate("2025-06-01 21:31:57")
                .initiator(new UserShortDto(1L, "Test User"))
                .paid(true)
                .title("Test Event")
                .rating(4.5) // Updated to include rating
                .build();
        List<EventShortDto> expectedList = List.of(eventDto);

        when(service.getFilteredEvents(any())).thenReturn(expectedList);

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/events")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("text", "0")
                        .param("categories", "1")
                        .param("paid", "true")
                        .param("rangeStart", "2022-01-06 13:30:38")
                        .param("rangeEnd", "2097-09-06 13:30:38")
                        .param("onlyAvailable", "false")
                        .param("sort", "RATING") // Updated to sort by RATING
                        .param("from", "0")
                        .param("size", "1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString();
        List<EventShortDto> actualList = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

        assertEquals(1, actualList.size(), "Response should contain one event");
        assertEquals(eventDto.getId(), actualList.getFirst().getId(), "Event ID should match");
        assertEquals("2025-06-01 21:31:57", actualList.getFirst().getEventDate(),
                "eventDate should be in format yyyy-MM-dd HH:mm:ss");
        assertEquals(eventDto.getRating(), actualList.getFirst().getRating(), "Rating should match");
    }

    @Test
    @SneakyThrows
    public void getFilteredEvents_whenCallMethodWithInvalidDate_thenThrow() {
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(List.of(mockInstance));

        when(service.getFilteredEvents(any()))
                .thenThrow(new IllegalArgumentException("Invalid date range"));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/events")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}