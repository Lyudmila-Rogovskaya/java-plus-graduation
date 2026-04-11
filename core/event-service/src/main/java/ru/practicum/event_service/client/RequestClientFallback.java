package ru.practicum.event_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.event_service.client.dto.ConfirmedRequestsDto;

import java.util.List;

@Slf4j
@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public List<ConfirmedRequestsDto> getConfirmedRequests(List<Long> eventIds) {
        log.warn("Сервис request-service недоступен, возвращается пустой список для eventIds: {}", eventIds);
        return List.of();
    }

    @Override
    public boolean hasUserVisitedEvent(Long userId, Long eventId) {
        log.warn("Сервис request-service недоступен, невозможно проверить посещение для пользователя {} и события {}", userId, eventId);
        return false;
    }

}
