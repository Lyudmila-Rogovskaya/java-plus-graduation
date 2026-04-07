package ru.practicum.request_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.request_service.client.dto.UserDto;
import ru.practicum.request_service.fallback.UserClientFallback;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);

}
