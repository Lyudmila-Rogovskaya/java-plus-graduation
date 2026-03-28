package ru.practicum.main_service.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main_service.event.dto.ModerationCommentDto;
import ru.practicum.main_service.event.service.ModerationCommentService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/users/{userId}/events/{eventId}")
public class PrivateModerationController {

    private final ModerationCommentService moderationCommentService;

    @GetMapping("/moderation-comments")
    public List<ModerationCommentDto> getModerationComments(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("Получение комментариев модерации для события с id: {} пользователем с id: {}",
                eventId, userId);
        return moderationCommentService.getCommentsByEventId(eventId);
    }

}
