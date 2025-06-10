package ru.practicum.user_service.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user_service.dto.NewUserRequest;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.exceptions.ServiceUnavailableException;

import java.util.List;

@Component
public class UserClientFallback {
    @PostMapping
    ResponseEntity<UserDto> addUser(@RequestBody @Valid NewUserRequest newUser) {
        throw new ServiceUnavailableException("UserService unavailable");
    }


    @GetMapping("/{userId}")
    ResponseEntity<UserShortDto> getUser(@PathVariable Long userId) {
        throw new ServiceUnavailableException("UserService unavailable");
    }
}
