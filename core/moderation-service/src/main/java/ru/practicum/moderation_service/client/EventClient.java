package ru.practicum.moderation_service.client; // новый

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.moderation_service.client.dto.EventDto;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventDto getEvent(@PathVariable("eventId") Long eventId);

}
