package ru.practicum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mapper.NewUserRequestMapper;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.AdminUserRepository;
import ru.practicum.user_service.dto.GetUsersDto;
import ru.practicum.user_service.dto.NewUserRequest;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.dto.UserShortDto;


import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final UserMapper userToDtoMapper;
    private final NewUserRequestMapper userShortMapper;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getUsers(GetUsersDto parameters) {
        log.info("\nAdminUserService.getAllUsers {}", parameters);
        int page = parameters.getFrom() / parameters.getSize();
        Pageable pageable = PageRequest.of(page, parameters.getSize());
        Page<User> response = parameters.getIds().isEmpty() ? adminUserRepository.findAll(pageable)
                : adminUserRepository.findByIds(parameters.getIds(), pageable);
        List<User> users = response.getContent().stream().toList();
        return userToDtoMapper.mapUsersListToDtoList(users);
    }

    @Override
    @Transactional
    public UserDto addUser(NewUserRequest newUserRequest) {
        log.info("\nAdminUserService.addUser {}", newUserRequest);
        User newUser = userShortMapper.mapNewUserRequestToUser(newUserRequest);
        return userToDtoMapper.mapUserToUserDto(adminUserRepository.save(newUser));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("\nDeleting user with id {}", id);
        getUser(id);
        adminUserRepository.deleteById(id);
    }

    @Override
    public UserShortDto getUser(Long userId) {
        return userMapper.toShortDto(adminUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with " + userId + " not found")));
    }
}
