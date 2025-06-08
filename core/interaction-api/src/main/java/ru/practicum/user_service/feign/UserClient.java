package ru.practicum.user_service.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request_service.feign.RequestClientFallback;
import ru.practicum.user_service.dto.NewUserRequest;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.dto.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users", fallbackFactory = UserClientFallback.class)
public interface UserClient {

    @PostMapping
    ResponseEntity<UserDto> addUser(@RequestBody @Valid NewUserRequest newUser);

    @GetMapping
    ResponseEntity<List<UserDto>> getUsers(@RequestParam(required = false, defaultValue = "") List<Long> ids,
                                           @RequestParam(required = false, defaultValue = "0") int from,
                                           @RequestParam(required = false, defaultValue = "10") int size);

    @DeleteMapping("/{userId}")
    ResponseEntity<String> deleteUser(@PathVariable
                                      @Min(value = 1, message = "ID must be positive") Long userId);

    @GetMapping("/{userId}")
    ResponseEntity<UserShortDto> getUser(@PathVariable Long userId);
}
