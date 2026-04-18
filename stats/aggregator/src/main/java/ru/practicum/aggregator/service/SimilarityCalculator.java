package ru.practicum.aggregator.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimilarityCalculator {

    public double calculateSimilarity(long eventA, long eventB, double sumMin, double totalA, double totalB) {
        if (totalA == 0 || totalB == 0) {
            return 0.0;
        }
        return sumMin / (Math.sqrt(totalA) * Math.sqrt(totalB));
    }

    public double getActionWeight(String actionType) {
        return switch (actionType) {
            case "VIEW" -> 0.4;
            case "REGISTER" -> 0.8;
            case "LIKE" -> 1.0;
            default -> 0.0;
        };
    }

    public long[] orderEventIds(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return new long[]{first, second};
    }

    public Map<Long, Double> getOrCreateInnerMap(ConcurrentHashMap<Long, Map<Long, Double>> outerMap, long key) {
        return outerMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
    }

}
