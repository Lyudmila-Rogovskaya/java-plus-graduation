package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.UserActionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionsConsumer {

    private final UserActionService userActionService;

    @KafkaListener(topics = "${kafka.topics.user-actions}",
            containerFactory = "userActionsKafkaListenerContainerFactory")
    public void consume(UserActionAvro message) {
        log.debug("Получено UserActionAvro: userId={}, eventId={}, action={}",
                message.getUserId(), message.getEventId(), message.getActionType());
        try {
            userActionService.processUserAction(message);
        } catch (Exception e) {
            log.error("Ошибка обработки UserActionAvro: {}", message, e);
        }
    }

}
