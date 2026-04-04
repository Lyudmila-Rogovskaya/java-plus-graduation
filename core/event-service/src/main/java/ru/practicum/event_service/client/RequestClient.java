package ru.practicum.event_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.event_service.client.dto.ConfirmedRequestsDto;

import java.util.List;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient {

    @GetMapping("/internal/requests/count")
    List<ConfirmedRequestsDto> getConfirmedRequests(@RequestParam("eventIds") List<Long> eventIds);

}
