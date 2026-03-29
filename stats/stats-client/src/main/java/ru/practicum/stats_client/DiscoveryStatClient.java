package ru.practicum.stats_client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.practicum.stat_dto.EndpointHitDto;
import ru.practicum.stat_dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true")
@Component
public class DiscoveryStatClient implements StatClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String STATS_SERVICE_ID = "stats-server";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final RetryTemplate retryTemplate;

    public DiscoveryStatClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.builder().build();

        RetryTemplate template = new RetryTemplate();
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        template.setBackOffPolicy(backOffPolicy);
        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy(3);
        template.setRetryPolicy(retryPolicy);
        this.retryTemplate = template;
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
            if (instances.isEmpty()) {
                throw new RuntimeException("No instances of stats-server found");
            }
            return instances.get(0);
        } catch (Exception e) {
            log.error("Ошибка обнаружения сервиса статистики с id: {}", STATS_SERVICE_ID, e);
            throw new RuntimeException("Ошибка обнаружения сервиса статистики", e);
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            URI uri = makeUri("/hit");
            restClient.post()
                    .uri(uri)
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Статистика успешно отправлена: app={}, uri={}", endpointHitDto.getApp(), endpointHitDto.getUri());
        } catch (Exception e) {
            log.error("Не удалось отправить статистику: app={}, uri={}", endpointHitDto.getApp(), endpointHitDto.getUri(), e);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {
            URI uri = makeUri("/stats");
            String url = uri.toString();
            StringBuilder sb = new StringBuilder(url);
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

            List<ViewStatsDto> stats = response != null ? List.of(response) : Collections.emptyList();
            log.debug("Статистика успешно получена, количество записей: {}", stats.size());
            return stats;
        } catch (Exception e) {
            log.error("Не удалось получить статистику", e);
            return Collections.emptyList();
        }
    }

}
