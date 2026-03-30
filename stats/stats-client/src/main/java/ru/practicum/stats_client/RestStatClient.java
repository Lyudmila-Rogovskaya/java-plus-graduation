package ru.practicum.stats_client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.practicum.stat_dto.EndpointHitDto;
import ru.practicum.stat_dto.ViewStatsDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
//@Primary
@ConditionalOnMissingBean(StatClient.class)
public class RestStatClient implements StatClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestClient restClient;
    private final String statServerUrl;

    public RestStatClient(@Value("${stat-server.url:http://localhost:9090}") String statServerUrl) {
        this.statServerUrl = statServerUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            restClient.post()
                    .uri(statServerUrl + "/hit")
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Статистика отправлена: {}", endpointHitDto);
        } catch (Exception e) {
            log.error("Не удалось отправить статистику: {}", e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {
            StringBuilder sb = new StringBuilder(statServerUrl + "/stats");
            sb.append("?start=").append(start.format(FORMATTER));
            sb.append("&end=").append(end.format(FORMATTER));
            if (uris != null && !uris.isEmpty()) {
                sb.append("&uris=").append(String.join(",", uris));
            }
            if (unique != null) {
                sb.append("&unique=").append(unique);
            }
            ViewStatsDto[] response = restClient.get()
                    .uri(sb.toString())
                    .retrieve()
                    .body(ViewStatsDto[].class);
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Не удалось получить статистику: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

}
