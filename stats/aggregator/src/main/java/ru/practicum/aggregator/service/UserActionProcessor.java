package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor {

    private final SimilarityService similarityService;

    @KafkaListener(topics = "${kafka.topics.user-actions}", containerFactory = "kafkaListenerContainerFactory")
    public void processUserAction(ConsumerRecord<Long, UserActionAvro> record) {
        UserActionAvro action = record.value();
        log.debug("Получено действие: userId={}, eventId={}, type={}", action.getUserId(), action.getEventId(), action.getActionType());
        similarityService.processUserAction(action);
    }

}
