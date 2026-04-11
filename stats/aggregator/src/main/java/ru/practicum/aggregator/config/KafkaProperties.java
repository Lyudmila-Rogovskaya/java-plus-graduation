package ru.practicum.aggregator.config;

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
    private ProducerConfig producer = new ProducerConfig();
    private Map<String, String> topics;

    @Data
    public static class ConsumerConfig {
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private int maxPollRecords;
        private boolean enableAutoCommit;
        private String autoOffsetReset;
    }

    @Data
    public static class ProducerConfig {
        private String keySerializer;
        private String valueSerializer;
    }

}
