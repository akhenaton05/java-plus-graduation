package ru.practicum.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.users.dto.ParticipationRequestDto;
import ru.practicum.users.model.ParticipationRequestStatus;
import ru.practicum.users.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PrivateRequestsForParticipationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParticipationRequestService participationRequestService;

    private final Long userId = 1L;
    private final Long eventId = 2L;
    private final Long requestId = 3L;

    public PrivateRequestsForParticipationControllerTest() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @SneakyThrows
    void getUserRequests_ShouldReturnListOfRequests() {
        List<ParticipationRequestDto> requests = List.of(
                new ParticipationRequestDto(requestId, userId, eventId, ParticipationRequestStatus.PENDING, LocalDateTime.now()));

        when(participationRequestService.getUserRequests(anyLong())).thenReturn(requests);

        var result = mockMvc.perform(get("/users/{userId}/requests", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(requests.size())))
                .andExpect(jsonPath("$[0].id", is(requestId.intValue())))
                .andExpect(jsonPath("$[0].requester", is(userId.intValue())))
                .andExpect(jsonPath("$[0].event", is(eventId.intValue())))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andReturn();
        System.out.println("Response: " + result.getResponse().getContentAsString());
    }

    @Test
    @SneakyThrows
    void addParticipationRequest_ShouldReturnCreatedRequest() {
        ParticipationRequestDto requestDto =
                new ParticipationRequestDto(requestId, userId, eventId, ParticipationRequestStatus.CONFIRMED, LocalDateTime.now());

        when(participationRequestService.addParticipationRequest(anyLong(), anyLong())).thenReturn(requestDto);

        var result = mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(requestId.intValue())))
                .andExpect(jsonPath("$.requester", is(userId.intValue())))
                .andExpect(jsonPath("$.event", is(eventId.intValue())))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andReturn();
        System.out.println("Response: " + result.getResponse().getContentAsString());
    }

    @Test
    @SneakyThrows
    void cancelRequest_ShouldReturnCanceledRequest() {
        ParticipationRequestDto canceledRequest =
                new ParticipationRequestDto(requestId, userId, eventId, ParticipationRequestStatus.CANCELED, LocalDateTime.now());

        when(participationRequestService.cancelRequest(anyLong(), anyLong())).thenReturn(canceledRequest);

        var result = mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(requestId.intValue())))
                .andExpect(jsonPath("$.requester", is(userId.intValue())))
                .andExpect(jsonPath("$.event", is(eventId.intValue())))
                .andExpect(jsonPath("$.status", is("CANCELED")))
                .andReturn();
        System.out.println("Response: " + result.getResponse().getContentAsString());
    }
}