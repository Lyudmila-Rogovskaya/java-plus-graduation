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
        log.info("Internal request: get confirmed requests count for eventIds: {}", eventIds);
        return requestService.getConfirmedRequestsCount(eventIds);
    }

}
