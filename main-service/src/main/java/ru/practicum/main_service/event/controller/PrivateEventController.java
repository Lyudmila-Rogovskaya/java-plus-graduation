package ru.practicum.main_service.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main_service.event.dto.EventFullDto;
import ru.practicum.main_service.event.dto.EventShortDto;
import ru.practicum.main_service.event.dto.NewEventDto;
import ru.practicum.main_service.event.dto.param.EventByUserRequest;
import ru.practicum.main_service.event.dto.param.EventsByUserParams;
import ru.practicum.main_service.event.dto.param.UpdateEventUserRequest;
import ru.practicum.main_service.event.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class PrivateEventController {

    private final EventService eventService;

    @GetMapping("/users/{userId}/events")
    public List<EventShortDto> getEventsByUser(@PathVariable Long userId, @PositiveOrZero @RequestParam(defaultValue = "0") Integer from, @Positive @RequestParam(defaultValue = "10") Integer size) {
        log.info("Получение событий пользователя с id: {}", userId);

        return eventService.getEventsByUser(new EventsByUserParams(userId, from, size));
    }

    @PostMapping("/users/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId, @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Создание события пользователем с id: {}", userId);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping("/users/{userId}/events/{eventId}")
    public EventFullDto getEventByUser(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("Получение события с id: {} пользователем с id: {}", eventId, userId);
        return eventService.getEventByUser(new EventByUserRequest(userId, eventId));
    }

    @PatchMapping("/users/{userId}/events/{eventId}")
    public EventFullDto updateEventByUser(@PathVariable Long userId, @PathVariable Long eventId, @Valid @RequestBody UpdateEventUserRequest updateEvent) {
        log.info("Обновление события с id: {} пользователем с id: {}", eventId, userId);
        return eventService.updateEventByUser(new EventByUserRequest(userId, eventId), updateEvent);
    }

}
