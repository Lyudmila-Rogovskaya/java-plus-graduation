package ru.practicum.stat_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.stat_dto.ViewStatsDto;
import ru.practicum.stat_server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatRepository extends JpaRepository<EndpointHit, Long> {

    @Query("""
            SELECT new ru.practicum.stat_dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip) as hits )
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR h.uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY hits DESC""")
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("""
            SELECT new ru.practicum.stat_dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip) as hits )
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR h.uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY hits DESC""")
    List<ViewStatsDto> getUniqueStats(LocalDateTime start, LocalDateTime end, List<String> uris);
}