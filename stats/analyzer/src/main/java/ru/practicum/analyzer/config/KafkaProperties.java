package ru.practicum.analyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private String bootstrapServers;
    private Map<String, ConsumerConfig> consumer;
    private Map<String, String> topics;

    @Data
    public static class ConsumerConfig {
        private String groupId;
        private String clientId;
        private int maxPollRecords = 500;
        private Map<String, String> properties;
    }

}
