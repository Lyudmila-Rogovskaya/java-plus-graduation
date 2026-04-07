package ru.practicum.user_service.user.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.user_service.user.dto.NewUserRequest;
import ru.practicum.user_service.user.dto.UserDto;
import ru.practicum.user_service.user.model.User;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long userId);

    User getEntityById(Long id);

    UserDto getUserById(Long userId);

}
