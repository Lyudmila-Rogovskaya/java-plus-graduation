package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.entity.EventSimilarityId;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId ORDER BY es.score DESC")
    List<EventSimilarity> findSimilarEvents(@Param("eventId") Long eventId);

    @Query("SELECT es FROM EventSimilarity es WHERE (es.eventA IN :eventIds OR es.eventB IN :eventIds) ORDER BY es.score DESC")
    List<EventSimilarity> findSimilarEventsForIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT es FROM EventSimilarity es " +
            "WHERE (es.eventA = :eventId AND es.eventB NOT IN :excludeIds) " +
            "   OR (es.eventB = :eventId AND es.eventA NOT IN :excludeIds) " +
            "ORDER BY es.score DESC")
    List<EventSimilarity> findSimilarEventsExcluding(@Param("eventId") Long eventId,
                                                     @Param("excludeIds") List<Long> excludeIds);

}
