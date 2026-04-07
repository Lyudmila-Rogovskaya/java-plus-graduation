package ru.practicum.request_service.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.request_service.client.UserClient;
import ru.practicum.request_service.client.dto.UserDto;

@Slf4j
@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUser(Long userId) {
        log.error("User-service is unavailable, cannot get user with id: {}", userId);
        throw new RuntimeException("User service is temporarily unavailable. Please try later.");
    }

}
