package ru.practicum.user_service.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.user_service.user.dto.NewUserRequest;
import ru.practicum.user_service.user.dto.UserDto;
import ru.practicum.user_service.user.dto.UserShortDto;
import ru.practicum.user_service.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toUser(NewUserRequest newUserRequest);

    UserDto toUserDto(User user);

    UserShortDto toUserShortDto(User user);

}
