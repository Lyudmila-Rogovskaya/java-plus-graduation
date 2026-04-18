package ru.practicum.event_service.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event_service.event.dto.EventFullDto;
import ru.practicum.event_service.event.dto.EventShortDto;
import ru.practicum.event_service.event.dto.param.EventsPublicParams;
import ru.practicum.event_service.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class PublicEventController {

    private final EventService eventService;

    @GetMapping("/events")
    public List<EventShortDto> getEventsPublic(@RequestParam(required = false) String text,
                                               @RequestParam(required = false) List<Long> categories,
                                               @RequestParam(required = false) Boolean paid,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                               @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                               @RequestParam(required = false) String sort,
                                               @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                               @Positive @RequestParam(defaultValue = "10") Integer size, HttpServletRequest request) {
        log.info("Публичный запрос событий");
        return eventService.getEventsPublic(new EventsPublicParams(text, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, sort, from, size, request));
    }

    @GetMapping("/events/{id}")
    public EventFullDto getEventPublic(@PathVariable Long id, HttpServletRequest request) {
        log.info("Публичный запрос события с id: {}", id);
        return eventService.getEventPublic(id, request);
    }

    @GetMapping("/events/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId,
                                                  @RequestParam(defaultValue = "10") @Positive int maxResults) {
        log.info("Запрос рекомендаций для пользователя {}", userId);
        return eventService.getRecommendations(userId, maxResults);
    }

    @PutMapping("/events/{eventId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likeEvent(@RequestHeader("X-EWM-USER-ID") Long userId,
                          @PathVariable Long eventId) {
        log.info("Пользователь {} лайкает событие {}", userId, eventId);
        eventService.likeEvent(userId, eventId);
    }

}
