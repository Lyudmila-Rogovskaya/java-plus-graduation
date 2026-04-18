package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_RECENT_ACTIONS_LIMIT = 10;
    private static final int MAX_ACTIONS_TO_LOAD = 100;

    private final UserActionService userActionService;
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<Long> interactedEventIds = userActionService.getUserActions(userId, MAX_ACTIONS_TO_LOAD)
                .stream().map(UserAction::getEventId).toList();

        if (interactedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventSimilarity> similarities = eventSimilarityRepository
                .findSimilarEventsExcluding(eventId, interactedEventIds);

        List<RecommendedEventProto> result = new ArrayList<>();
        for (EventSimilarity es : similarities) {
            long otherEventId = (es.getEventA() == eventId) ? es.getEventB() : es.getEventA();
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(otherEventId)
                    .setScore(es.getScore())
                    .build());
            if (result.size() >= maxResults) break;
        }
        return result;
    }

    public List<RecommendedEventProto> getUserPredictions(long userId, int maxResults) {
        List<UserAction> recentActions = userActionService.getUserActions(userId, DEFAULT_RECENT_ACTIONS_LIMIT);
        if (recentActions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> interactedEventIds = recentActions.stream()
                .map(UserAction::getEventId).collect(Collectors.toSet());
        List<Long> seedEventIds = recentActions.stream()
                .map(UserAction::getEventId).toList();

        Map<Long, CandidateAccumulator> candidateMap = buildCandidateMap(seedEventIds, interactedEventIds, recentActions);

        return convertCandidatesToResult(candidateMap, maxResults);
    }

    private Map<Long, CandidateAccumulator> buildCandidateMap(List<Long> seedEventIds,
                                                              Set<Long> interactedEventIds,
                                                              List<UserAction> recentActions) {
        List<EventSimilarity> allSimilarities = eventSimilarityRepository.findSimilarEventsForIds(seedEventIds);
        Map<Long, CandidateAccumulator> candidateMap = new HashMap<>();

        for (EventSimilarity es : allSimilarities) {
            long eventA = es.getEventA();
            long eventB = es.getEventB();
            double score = es.getScore();

            Long candidateId = null;
            Long seedId = null;
            if (!interactedEventIds.contains(eventA) && interactedEventIds.contains(eventB)) {
                candidateId = eventA;
                seedId = eventB;
            } else if (!interactedEventIds.contains(eventB) && interactedEventIds.contains(eventA)) {
                candidateId = eventB;
                seedId = eventA;
            }

            if (candidateId != null && seedId != null) {
                double userWeight = findUserWeightForEvent(recentActions, seedId);
                candidateMap.computeIfAbsent(candidateId, k -> new CandidateAccumulator()).add(score, userWeight);
            }
        }
        return candidateMap;
    }

    private double findUserWeightForEvent(List<UserAction> recentActions, long eventId) {
        return recentActions.stream()
                .filter(ua -> ua.getEventId().equals(eventId))
                .mapToDouble(UserAction::getWeight)
                .findFirst()
                .orElse(0.0);
    }

    private List<RecommendedEventProto> convertCandidatesToResult(Map<Long, CandidateAccumulator> candidateMap, int maxResults) {
        return candidateMap.entrySet().stream()
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue().getSumWeighted() / e.getValue().getSumSimilarity())
                        .build())
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    private static class CandidateAccumulator {
        private double sumSimilarity = 0.0;
        private double sumWeighted = 0.0;

        void add(double similarity, double weight) {
            sumSimilarity += similarity;
            sumWeighted += similarity * weight;
        }

        double getSumSimilarity() {
            return sumSimilarity;
        }

        double getSumWeighted() {
            return sumWeighted;
        }
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserAction> actions = userActionRepository.findByEventIdIn(eventIds);
        Map<Long, Double> sumWeights = actions.stream()
                .collect(Collectors.groupingBy(
                        UserAction::getEventId,
                        Collectors.summingDouble(UserAction::getWeight)
                ));
        return sumWeights.entrySet().stream()
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

}
