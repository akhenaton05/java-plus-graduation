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
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.config.StatsClientConfig;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.LookEventDto;
import ru.practicum.events.dto.SearchEventsParams;
import ru.practicum.events.model.Location;
import ru.practicum.events.model.StateEvent;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.service.PublicEventsService;
import ru.practicum.events.service.PublicEventsServiceImpl;
import ru.practicum.users.dto.UserShortDto;

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
    private PublicEventsServiceImpl publicEventsServiceImpl;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private StatsClientConfig statsClientConfig;

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
            .views(0)
            .createdOn(secondDate)
            .description("12345".repeat(6))
            .publishedOn(thirdDate)
            .location(new Location())
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
            .views(0)
            .build();

    @Test
    @SneakyThrows
    public void getEventInfo_whenValidParams_thenGetResponse() {
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(List.of(mockInstance));

        when(service.getEventInfo(any())).thenReturn(eventFullDto);
        long id = 1L;

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/events/{id}", id)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(mvcResult.getResponse());

        ArgumentCaptor<LookEventDto> lookEventDtoCaptor = ArgumentCaptor.forClass(LookEventDto.class);

        verify(service).getEventInfo(lookEventDtoCaptor.capture());
        assertEquals(1L, lookEventDtoCaptor.getValue().getId());
        assertNotNull(lookEventDtoCaptor.getValue().getIp());
        assertNotNull(lookEventDtoCaptor.getValue().getUri());
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

        ArgumentCaptor<SearchEventsParams> searchParam = ArgumentCaptor.forClass(SearchEventsParams.class);
        ArgumentCaptor<LookEventDto> lookEventDtoCaptor = ArgumentCaptor.forClass(LookEventDto.class);
        when(service.getFilteredEvents(any(), any())).thenReturn(expectedList);

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

        verify(service).getFilteredEvents(searchParam.capture(), lookEventDtoCaptor.capture());

        assertNull(lookEventDtoCaptor.getValue().getId());
        assertNotNull(lookEventDtoCaptor.getValue().getIp());
        assertNotNull(lookEventDtoCaptor.getValue().getUri());

        assertEquals("", searchParam.getValue().getText(), "text");
        assertTrue(searchParam.getValue().getCategories().isEmpty(), "categories");
        assertNull(searchParam.getValue().getPaid(), "paid");
        assertEquals(thirdDate, searchParam.getValue().getRangeStart(), "rangeStart");
        assertEquals(secondDate, searchParam.getValue().getRangeEnd(), "rangeEnd");
        assertFalse(searchParam.getValue().getOnlyAvailable(), "onlyAvailable");
        assertEquals("EVENT_DATE", searchParam.getValue().getSort(), "sort");
        assertEquals(0, searchParam.getValue().getFrom(), "from");
        assertEquals(10, searchParam.getValue().getSize(), "size");
    }

    @Test
    @SneakyThrows
    public void getFilteredEvents_whenValidParams_thenReturnCorrectEventDateFormat() {
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(Arrays.asList(mockInstance));

        // Подготовка тестового EventShortDto
        EventShortDto eventDto = EventShortDto.builder()
                .id(1L)
                .annotation("Test event")
                .category(new CategoryDto(1L, "Test Category"))
                .confirmedRequests(0)
                .eventDate("2025-06-01 21:31:57") // Ожидаемый формат
                .initiator(new UserShortDto(1L, "Test User"))
                .paid(true)
                .title("Test Event")
                .views(0)
                .build();
        List<EventShortDto> expectedList = List.of(eventDto);

        // Мокаем сервис
        when(service.getFilteredEvents(any(), any())).thenReturn(expectedList);

        // Выполняем запрос с исправленным параметром categories
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/events")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("text", "0")
                        .param("categories", "1") // Изменено с "0" на "1"
                        .param("paid", "true")
                        .param("rangeStart", "2022-01-06 13:30:38")
                        .param("rangeEnd", "2097-09-06 13:30:38")
                        .param("onlyAvailable", "false")
                        .param("sort", "EVENT_DATE")
                        .param("from", "0")
                        .param("size", "1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Проверяем ответ
        String jsonResponse = mvcResult.getResponse().getContentAsString();
        System.out.println("JSON Response: " + jsonResponse); // Для отладки
        List<EventShortDto> actualList = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

        assertEquals(1, actualList.size(), "Response should contain one event");
        assertEquals(eventDto.getId(), actualList.getFirst().getId(), "Event ID should match");
        assertEquals("2025-06-01 21:31:57", actualList.getFirst().getEventDate(),
                "eventDate should be in format yyyy-MM-dd HH:mm:ss");
    }

    @Test
    @SneakyThrows
    public void getFilteredEvents_whenCallMethodWithInvalidDate_thenThrow() {
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(List.of(mockInstance));

        when(service.getFilteredEvents(any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid date range"));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/events")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}