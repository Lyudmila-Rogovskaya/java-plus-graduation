package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.entity.UserActionId;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionService {

    private final UserActionRepository userActionRepository;

    @Transactional
    public void processUserAction(UserActionAvro avro) {
        Long userId = avro.getUserId();
        Long eventId = avro.getEventId();
        double weight = mapActionToWeight(avro.getActionType());

        UserAction existing = userActionRepository.findById(new UserActionId(userId, eventId))
                .orElse(null);

        if (existing == null) {
            UserAction newAction = UserAction.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .weight(weight)
                    .lastUpdated(Instant.ofEpochMilli(avro.getTimestamp().toEpochMilli()))
                    .build();
            userActionRepository.save(newAction);
            log.debug("Сохранено новое действие пользователя: userId={}, eventId={}, weight={}", userId, eventId, weight);
        } else {
            if (weight > existing.getWeight()) {
                existing.setWeight(weight);
                existing.setLastUpdated(Instant.ofEpochMilli(avro.getTimestamp().toEpochMilli()));
                userActionRepository.save(existing);
                log.debug("Обновлён вес действия пользователя: userId={}, eventId={}, newWeight={}", userId, eventId, weight);
            }
        }
    }

    private double mapActionToWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> 0.4;
            case ACTION_REGISTER -> 0.8;
            case ACTION_LIKE -> 1.0;
            default -> throw new IllegalArgumentException("Unknown action type: " + actionType);
        };
    }

    public List<UserAction> getUserActions(Long userId, int limit) {
        return userActionRepository.findAllByUserIdOrderByLastUpdatedDesc(userId)
                .stream().limit(limit).toList();
    }

}
