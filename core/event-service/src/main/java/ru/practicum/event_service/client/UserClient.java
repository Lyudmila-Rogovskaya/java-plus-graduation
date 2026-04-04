package ru.practicum.event_service.client; // новый

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.event_service.user.dto.UserDto;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);

}
