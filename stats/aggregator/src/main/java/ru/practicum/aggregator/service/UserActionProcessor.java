package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor {

    private final KafkaTemplate<String, EventsSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String similarityTopic;

    private final Map<Long, Map<Long, Double>> userWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> totalWeights = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();

    private static final double WEIGHT_VIEW = 0.4;
    private static final double WEIGHT_REGISTER = 0.8;
    private static final double WEIGHT_LIKE = 1.0;

    @KafkaListener(topics = "${kafka.topics.user-actions}", containerFactory = "kafkaListenerContainerFactory")
    public void processUserAction(ConsumerRecord<Long, UserActionAvro> record) {
        UserActionAvro action = record.value();
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newWeight = mapActionToWeight(action.getActionType());

        log.debug("Обработка действия пользователя: userId={}, eventId={}, weight={}", userId, eventId, newWeight);

        Map<Long, Double> eventUserWeights = userWeights.computeIfAbsent(eventId,
                k -> new ConcurrentHashMap<>());
        Double oldWeight = eventUserWeights.get(userId);
        if (oldWeight == null) oldWeight = 0.0;

        if (newWeight <= oldWeight) {
            log.debug("Новый вес {} не превышает старый {}, пропускаем", newWeight, oldWeight);
            return;
        }

        eventUserWeights.put(userId, newWeight);

        double delta = newWeight - oldWeight;
        totalWeights.merge(eventId, delta, Double::sum);

        updateSimilarities(eventId, userId, oldWeight, newWeight, action.getTimestamp().toEpochMilli());
        recalcAndSendSimilarities(eventId, action.getTimestamp().toEpochMilli());
    }

    private double mapActionToWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> WEIGHT_VIEW;
            case REGISTER -> WEIGHT_REGISTER;
            case LIKE -> WEIGHT_LIKE;
            default -> 0.0;
        };
    }

    private void updateSimilarities(long eventA, long userId, double oldWeight, double newWeight, long timestamp) {
        for (Map.Entry<Long, Map<Long, Double>> entry : userWeights.entrySet()) {
            long eventB = entry.getKey();
            if (eventB == eventA) continue;

            Double weightB = entry.getValue().get(userId);
            if (weightB == null) continue;

            double oldMin = Math.min(oldWeight, weightB);
            double newMin = Math.min(newWeight, weightB);
            double deltaMin = newMin - oldMin;

            if (deltaMin != 0) {
                long first = Math.min(eventA, eventB);
                long second = Math.max(eventA, eventB);
                minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                        .merge(second, deltaMin, Double::sum);
                log.trace("Обновлена сумма S_min для пары ({}, {}): дельта = {}", first, second, deltaMin);
            }
        }
    }

    private void recalcAndSendSimilarities(long eventA, long timestamp) {
        double totalA = totalWeights.getOrDefault(eventA, 0.0);
        if (totalA == 0) {
            return;
        }

        for (Map.Entry<Long, Map<Long, Double>> entry : minSums.entrySet()) {
            long first = entry.getKey();
            Map<Long, Double> inner = entry.getValue();

            if (first == eventA) {
                for (Map.Entry<Long, Double> pair : inner.entrySet()) {
                    long second = pair.getKey();
                    double sMin = pair.getValue();
                    double totalB = totalWeights.getOrDefault(second, 0.0);
                    if (totalB == 0) {
                        continue;
                    }
                    double similarity = sMin / (Math.sqrt(totalA) * Math.sqrt(totalB));
                    sendSimilarity(eventA, second, similarity, timestamp);
                }
            } else if (inner.containsKey(eventA)) {
                double sMin = inner.get(eventA);
                double totalB = totalWeights.getOrDefault(first, 0.0);
                if (totalB == 0) {
                    continue;
                }
                double similarity = sMin / (Math.sqrt(totalA) * Math.sqrt(totalB));
                sendSimilarity(first, eventA, similarity, timestamp);
            }
        }
    }

    private void sendSimilarity(long eventA, long eventB, double similarity, long timestamp) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        EventsSimilarityAvro avro = EventsSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(similarity)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .build();
        kafkaTemplate.send(similarityTopic, first + "-" + second, avro);
        log.debug("Отправлено сходство: eventA={}, eventB={}, score={}", first, second, similarity);
    }

}
