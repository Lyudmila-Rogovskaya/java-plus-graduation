package ru.practicum.main_service.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main_service.event.dto.EventFullDtoWithModeration;
import ru.practicum.main_service.event.dto.param.UpdateEventAdminRequestWithComment;
import ru.practicum.main_service.event.service.EventServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/admin")
public class AdminModerationController {

    private final EventServiceImpl eventService;

    @GetMapping("/events/moderation")
    public List<EventFullDtoWithModeration> getEventsForModeration(
            @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
            @Positive @RequestParam(defaultValue = "10") Integer size) {

        log.info("Получение событий для модерации, from={}, size={}", from, size);
        return eventService.getEventsForModeration(from, size);
    }

    @PatchMapping("/events/{eventId}/moderate")
    public EventFullDtoWithModeration moderateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequestWithComment updateRequest) {

        log.info("Модерация события с id: {}", eventId);
        return eventService.updateEventByAdminWithComment(eventId, updateRequest);
    }

}
