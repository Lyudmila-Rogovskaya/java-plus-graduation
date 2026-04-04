package ru.practicum.event_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.event_service.client.dto.UserDto;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);

}
