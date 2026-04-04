package ru.practicum.event_service.client; // новый

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.event_service.client.dto.CreateCommentRequest;
import ru.practicum.event_service.event.dto.ModerationCommentDto;

import java.util.List;

@Slf4j
@Component
public class ModerationCommentClientFallback implements ModerationCommentClient {

    @Override
    public List<ModerationCommentDto> getCommentsByEventIds(List<Long> eventIds) {
        log.warn("Moderation-service is unavailable, returning empty list for eventIds: {}", eventIds);
        return List.of();
    }

    @Override
    public ModerationCommentDto createComment(CreateCommentRequest request) {
        log.warn("Moderation-service is unavailable, cannot create comment for event {}", request.getEventId());
        throw new RuntimeException("Moderation service is temporarily unavailable");
    }

}
