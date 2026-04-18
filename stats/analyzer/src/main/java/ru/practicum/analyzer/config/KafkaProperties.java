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
    private ConsumerConfig consumer = new ConsumerConfig();
    private Map<String, String> topics;

    @Data
    public static class ConsumerConfig {
        private String userActionsGroupId;
        private String userActionsClientId;
        private int userActionsMaxPollRecords = 500;
        private Map<String, String> userActionsProperties;

        private String eventsSimilarityGroupId;
        private String eventsSimilarityClientId;
        private int eventsSimilarityMaxPollRecords = 500;
        private Map<String, String> eventsSimilarityProperties;
    }

}
