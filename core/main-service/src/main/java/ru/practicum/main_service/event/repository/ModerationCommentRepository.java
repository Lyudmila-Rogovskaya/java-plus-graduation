package ru.practicum.main_service.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.main_service.event.model.ModerationComment;

import java.util.List;

@Repository
public interface ModerationCommentRepository extends JpaRepository<ModerationComment, Long> {

    List<ModerationComment> findByEventIdOrderByCreatedOnDesc(Long eventId);

    List<ModerationComment> findAllByEventIdIn(List<Long> eventIds);
}
