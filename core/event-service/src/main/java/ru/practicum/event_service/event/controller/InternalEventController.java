package ru.practicum.event_service.event.controller; // новый

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.event_service.event.dto.EventDto;
import ru.practicum.event_service.event.dto.EventValidationDto;
import ru.practicum.event_service.event.service.EventService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}/validate")
    public EventValidationDto validateEvent(@PathVariable Long eventId) {
        log.info("Internal request: validate event {}", eventId);
        return eventService.validateEvent(eventId);
    }

    @GetMapping("/{eventId}")
    public EventDto getEvent(@PathVariable Long eventId) {
        return eventService.getEventDto(eventId);
    }

}
