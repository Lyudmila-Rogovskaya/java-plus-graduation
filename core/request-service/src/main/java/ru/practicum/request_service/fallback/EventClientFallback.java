package ru.practicum.request_service.fallback; // новый

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.request_service.client.EventClient;
import ru.practicum.request_service.dto.EventValidationDto;

@Slf4j
@Component
public class EventClientFallback implements EventClient {

    @Override
    public EventValidationDto validateEvent(Long eventId) {
        log.error("Event service is unavailable, cannot validate event with id: {}", eventId);
        throw new RuntimeException("Event service is temporarily unavailable. Please try later.");
    }

}
