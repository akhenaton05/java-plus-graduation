package ru.practicum.comments;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.MainService;
import ru.practicum.comments.service.CommentService;
import ru.practicum.config.StatsClientConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MainService.class)
@AutoConfigureMockMvc
public class AdminCommentsControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CommentService commentService;
    @MockBean
    private DiscoveryClient discoveryClient;
    @MockBean
    private StatsClientConfig statsClientConfig;

    @Test
    @SneakyThrows
    public void deleteByIdTest() {
        // Настройка моков
        when(statsClientConfig.getServiceId()).thenReturn("stats-service");
        ServiceInstance mockInstance = mock(ServiceInstance.class);
        when(mockInstance.getHost()).thenReturn("localhost");
        when(mockInstance.getPort()).thenReturn(9090);
        when(discoveryClient.getInstances("stats-service")).thenReturn(java.util.List.of(mockInstance));

        mockMvc.perform(delete("/admin/comments/" + 1L))
                .andExpect(status().isNoContent());
    }
}