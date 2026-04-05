package ru.practicum.moderation_service.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.moderation_service.event.dto.ModerationCommentDto;
import ru.practicum.moderation_service.event.service.ModerationCommentService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/comments")
public class InternalModerationController {

    private final ModerationCommentService commentService;

    @GetMapping
    public List<ModerationCommentDto> getCommentsByEventIds(@RequestParam List<Long> eventIds) {
        log.info("Internal request: get comments for eventIds: {}", eventIds);
        return commentService.getCommentsByEventIds(eventIds);
    }

    @PostMapping
    public ModerationCommentDto createComment(@RequestBody CreateCommentRequest request) {
        log.info("Internal request: create comment for event {} by admin {}", request.getEventId(), request.getAdminId());
        return commentService.createComment(request.getEventId(), request.getAdminId(), request.getCommentText());
    }

    @DeleteMapping("/{eventId}")
    public void deleteCommentsByEventId(@PathVariable Long eventId) {
        log.info("Internal request: delete comments for eventId: {}", eventId);
        commentService.deleteCommentsByEventId(eventId);
    }

}
