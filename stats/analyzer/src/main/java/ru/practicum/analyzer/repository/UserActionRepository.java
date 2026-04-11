package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.entity.UserActionId;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, UserActionId> {

    List<UserAction> findAllByUserIdOrderByLastUpdatedDesc(Long userId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.userId = :userId AND ua.eventId IN :eventIds")
    List<UserAction> findByUserIdAndEventIdIn(@Param("userId") Long userId, @Param("eventIds") List<Long> eventIds);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.eventId IN :eventIds")
    List<UserAction> findByEventIdIn(@Param("eventIds") List<Long> eventIds);

}
