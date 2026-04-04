package ru.practicum.event_service.client; // новый

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event_service.client.dto.CreateCommentRequest;
import ru.practicum.event_service.event.dto.ModerationCommentDto;

import java.util.List;

@FeignClient(name = "moderation-service", fallback = ModerationCommentClientFallback.class)
public interface ModerationCommentClient {

    @GetMapping("/internal/comments")
    List<ModerationCommentDto> getCommentsByEventIds(@RequestParam("eventIds") List<Long> eventIds);

    @PostMapping("/internal/comments")
    ModerationCommentDto createComment(@RequestBody CreateCommentRequest request);

    @DeleteMapping("/internal/comments/{eventId}")
    void deleteCommentsByEventId(@PathVariable("eventId") Long eventId);

}
