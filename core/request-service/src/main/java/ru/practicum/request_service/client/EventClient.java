package ru.practicum.request_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.request_service.dto.EventValidationDto;
import ru.practicum.request_service.fallback.EventClientFallback;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}/validate")
    EventValidationDto validateEvent(@PathVariable("eventId") Long eventId);

}
