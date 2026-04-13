package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {

    private static final double WEIGHT_VIEW = 0.4;
    private static final double WEIGHT_REGISTER = 0.8;
    private static final double WEIGHT_LIKE = 1.0;

    private final Map<Long, Map<Long, Double>> weightMatrix = new ConcurrentHashMap<>();
    private final Map<Long, Double> totalWeightByEvent = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> eventsByUser = new ConcurrentHashMap<>();

    private final KafkaTemplate<String, EventsSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String similarityTopic;

    public void processUserAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double actionWeight = mapActionToWeight(action.getActionType());

        Map<Long, Double> userWeightsForEvent = weightMatrix.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        double oldWeight = userWeightsForEvent.getOrDefault(userId, 0.0);

        if (actionWeight <= oldWeight) {
            log.debug("Новый вес {} не превышает старый {} для userId={}, eventId={}. Пропуск.", actionWeight, oldWeight, userId, eventId);
            return;
        }

        userWeightsForEvent.put(userId, actionWeight);
        weightMatrix.put(eventId, userWeightsForEvent);

        double delta = actionWeight - oldWeight;
        totalWeightByEvent.merge(eventId, delta, Double::sum);

        Set<Long> userEvents = eventsByUser.computeIfAbsent(userId, k -> new HashSet<>());

        List<EventsSimilarityAvro> similaritiesToSend = new ArrayList<>();
        for (Long otherEventId : userEvents) {
            if (otherEventId == eventId) continue;

            double otherWeight = weightMatrix.getOrDefault(otherEventId, Map.of()).getOrDefault(userId, 0.0);
            if (otherWeight == 0.0) continue;

            double newSimilarity = recalculatePair(eventId, otherEventId, userId, oldWeight, actionWeight, otherWeight);
            similaritiesToSend.add(mapToAvro(eventId, otherEventId, newSimilarity, action.getTimestamp()));
        }

        userEvents.add(eventId);

        for (EventsSimilarityAvro similarity : similaritiesToSend) {
            String key = similarity.getEventA() + "-" + similarity.getEventB();
            kafkaTemplate.send(similarityTopic, key, similarity);
            log.debug("Отправлено сходство: eventA={}, eventB={}, score={}", similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        }
    }

    private double recalculatePair(long eventA, long eventB, long userId,
                                   double oldWeightA, double newWeightA, double weightB) {
        double oldContribution = Math.min(oldWeightA, weightB);
        double newContribution = Math.min(newWeightA, weightB);
        double deltaMin = newContribution - oldContribution;

        if (deltaMin == 0.0) {
        }

        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        double currentMinSum = minSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
        double newMinSum = currentMinSum + deltaMin;
        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>()).put(second, newMinSum);

        double totalA = totalWeightByEvent.getOrDefault(eventA, 0.0);
        double totalB = totalWeightByEvent.getOrDefault(eventB, 0.0);

        if (totalA == 0.0 || totalB == 0.0) {
            return 0.0;
        }
        return newMinSum / (Math.sqrt(totalA) * Math.sqrt(totalB));
    }

    private EventsSimilarityAvro mapToAvro(long eventA, long eventB, double score, Instant timestamp) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return EventsSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();
    }

    private double mapActionToWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> WEIGHT_VIEW;
            case REGISTER -> WEIGHT_REGISTER;
            case LIKE -> WEIGHT_LIKE;
            default -> 0.0;
        };
    }

}
