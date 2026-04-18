package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.EventSimilarityService;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventsSimilarityConsumer {

    private final EventSimilarityService eventSimilarityService;

    @KafkaListener(topics = "${kafka.topics.events-similarity}",
            containerFactory = "eventsSimilarityKafkaListenerContainerFactory")
    public void consume(EventsSimilarityAvro message) {
        log.debug("Получено сообщение EventsSimilarityAvro: eventA={}, eventB={}, score={}",
                message.getEventA(), message.getEventB(), message.getScore());
        try {
            eventSimilarityService.processSimilarity(message);
        } catch (Exception e) {
            log.error("Ошибка обработки EventsSimilarityAvro: {}", message, e);
        }
    }

}
