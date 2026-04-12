package ru.practicum.analyzer.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.serialization.EventsSimilarityAvroDeserializer;
import ru.practicum.ewm.stats.avro.serialization.UserActionAvroDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    private <T> ConsumerFactory<String, T> consumerFactory(
            Class<?> valueDeserializer,
            String groupId,
            String clientId,
            int maxPollRecords,
            Map<String, String> additionalProps) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        if (additionalProps != null) {
            props.putAll(additionalProps);
        }
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro>
    userActionsKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(
                UserActionAvroDeserializer.class,
                kafkaProperties.getConsumer().getUserActionsGroupId(),
                kafkaProperties.getConsumer().getUserActionsClientId(),
                kafkaProperties.getConsumer().getUserActionsMaxPollRecords(),
                kafkaProperties.getConsumer().getUserActionsProperties()
        ));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventsSimilarityAvro>
    eventsSimilarityKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, EventsSimilarityAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(
                EventsSimilarityAvroDeserializer.class,
                kafkaProperties.getConsumer().getEventsSimilarityGroupId(),
                kafkaProperties.getConsumer().getEventsSimilarityClientId(),
                kafkaProperties.getConsumer().getEventsSimilarityMaxPollRecords(),
                kafkaProperties.getConsumer().getEventsSimilarityProperties()
        ));
        return factory;
    }

}
