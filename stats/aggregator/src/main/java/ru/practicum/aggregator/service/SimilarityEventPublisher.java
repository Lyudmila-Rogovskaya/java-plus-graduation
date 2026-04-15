package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityEventPublisher {

    private final KafkaTemplate<String, EventsSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String similarityTopic;

    public void send(EventsSimilarityAvro similarity) {
        String key = similarity.getEventA() + "-" + similarity.getEventB();
        kafkaTemplate.send(similarityTopic, key, similarity);
        log.debug("Отправлено сходство: eventA={}, eventB={}, score={}", similarity.getEventA(), similarity.getEventB(), similarity.getScore());
    }

}
