package ru.practicum.request_service.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.request_service.request.dto.ConfirmedRequestsDto;
import ru.practicum.request_service.request.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/count")
    public List<ConfirmedRequestsDto> getConfirmedRequests(@RequestParam List<Long> eventIds) {
        log.info("Внутренний запрос: получение количества подтверждённых заявок для eventIds: {}", eventIds);
        return requestService.getConfirmedRequestsCount(eventIds);
    }

    @GetMapping("/visited")
    public boolean hasUserVisitedEvent(@RequestParam Long userId, @RequestParam Long eventId) {
        log.info("Внутренний запрос: проверка, посещал ли пользователь {} событие {}", userId, eventId);
        return requestService.hasUserVisitedEvent(userId, eventId);
    }

}
