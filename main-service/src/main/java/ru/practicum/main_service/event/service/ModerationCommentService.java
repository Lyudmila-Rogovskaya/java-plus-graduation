package ru.practicum.main_service.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main_service.event.dto.ModerationCommentDto;
import ru.practicum.main_service.event.mapper.ModerationCommentMapper;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.model.ModerationComment;
import ru.practicum.main_service.event.repository.ModerationCommentRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModerationCommentService {

    private final ModerationCommentRepository moderationCommentRepository;
    private final ModerationCommentMapper moderationCommentMapper;

    @Transactional
    public ModerationCommentDto createComment(Event event, Long adminId, String commentText) {
        ModerationComment comment = ModerationComment.builder()
                .event(event)
                .adminId(adminId)
                .commentText(commentText)
                .build();

        ModerationComment savedComment = moderationCommentRepository.save(comment);
        log.info("Создан комментарий модерации с id: {} для события с id: {}",
                savedComment.getId(), event.getId());

        return moderationCommentMapper.toDto(savedComment);
    }

    public List<ModerationCommentDto> getCommentsByEventId(Long eventId) {
        List<ModerationComment> comments = moderationCommentRepository
                .findByEventIdOrderByCreatedOnDesc(eventId);

        return comments.stream()
                .map(moderationCommentMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteCommentsByEventId(Long eventId) {
        List<ModerationComment> comments = moderationCommentRepository
                .findByEventIdOrderByCreatedOnDesc(eventId);

        if (!comments.isEmpty()) {
            moderationCommentRepository.deleteAll(comments);
            log.info("Удалено {} комментариев модерации для события с id: {}",
                    comments.size(), eventId);
        }
    }

    public List<ModerationCommentDto> getCommentsByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<ModerationComment> comments = moderationCommentRepository.findAllByEventIdIn(eventIds);
        return moderationCommentMapper.toDtoList(comments);
    }

}
