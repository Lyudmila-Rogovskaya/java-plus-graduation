package ru.practicum.event_service.client; // новый

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.event_service.client.dto.ConfirmedRequestsDto;

import java.util.List;

@Slf4j
@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public List<ConfirmedRequestsDto> getConfirmedRequests(List<Long> eventIds) {
        log.warn("Request-service is unavailable, returning default empty list for eventIds: {}", eventIds);
        return List.of();
    }

}
