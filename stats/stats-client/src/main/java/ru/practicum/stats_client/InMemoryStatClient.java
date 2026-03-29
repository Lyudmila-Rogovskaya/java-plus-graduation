package ru.practicum.stats_client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.practicum.stat_dto.EndpointHitDto;
import ru.practicum.stat_dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Profile("test")
public class InMemoryStatClient implements StatClient {

    private final Map<String, Set<String>> hits = new ConcurrentHashMap<>();

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        String uri = endpointHitDto.getUri();
        String ip = endpointHitDto.getIp();
        hits.computeIfAbsent(uri, k -> ConcurrentHashMap.newKeySet()).add(ip);
        log.debug("In-memory hit recorded: uri={}, ip={}", uri, ip);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        List<ViewStatsDto> result = new ArrayList<>();
        for (String uri : uris) {
            Set<String> ips = hits.getOrDefault(uri, Collections.emptySet());
            long count = unique ? ips.size() : ips.size();
            if (count > 0) {
                result.add(new ViewStatsDto("ewm-main-service", uri, count));
            }
        }

        result.sort((a, b) -> Long.compare(b.getHits(), a.getHits()));
        log.debug("In-memory getStats returned {} records for uris={}", result.size(), uris);
        return result;
    }

}
