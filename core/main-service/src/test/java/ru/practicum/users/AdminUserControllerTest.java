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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.users.dto.GetUsersDto;
import ru.practicum.users.dto.NewUserRequest;
import ru.practicum.users.dto.UserDto;
import ru.practicum.users.service.AdminUserService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @MockBean
    private AdminUserService adminUserService;

    private final UserDto userDto = new UserDto(5L, "User", "User@user.com");
    private final UserDto userDtoSecond = new UserDto(12L, "User1", "User1@user.com");
    private final NewUserRequest newUserRequest = new NewUserRequest("User", "User@user.com");

    @Test
    @SneakyThrows
    void addUser_shouldReturnCreatedUser() {
        when(adminUserService.addUser(any(NewUserRequest.class))).thenReturn(userDto);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(userDto.getId().intValue())))
                .andExpect(jsonPath("$.name", is(userDto.getName())))
                .andExpect(jsonPath("$.email", is(userDto.getEmail())));

        verify(adminUserService, times(1)).addUser(any(NewUserRequest.class));
    }

    @Test
    @SneakyThrows
    void getUsers_shouldReturnUserList() {
        when(adminUserService.getUsers(any(GetUsersDto.class))).thenReturn(List.of(userDto, userDtoSecond));

        mockMvc.perform(get("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].id", is(userDto.getId().intValue())))
                .andExpect(jsonPath("$[0].name", is(userDto.getName())))
                .andExpect(jsonPath("$[0].email", is(userDto.getEmail())))
                .andExpect(jsonPath("$[1].id", is(userDtoSecond.getId().intValue())))
                .andExpect(jsonPath("$[1].name", is(userDtoSecond.getName())))
                .andExpect(jsonPath("$[1].email", is(userDtoSecond.getEmail())));

        verify(adminUserService, times(1)).getUsers(any(GetUsersDto.class));
    }

    @Test
    @SneakyThrows
    void deleteUser_shouldReturnNoContent() {
        mockMvc.perform(delete("/admin/users/{userId}", userDto.getId()))
                .andExpect(status().isNoContent());

        verify(adminUserService, times(1)).deleteUser(userDto.getId());
    }
}