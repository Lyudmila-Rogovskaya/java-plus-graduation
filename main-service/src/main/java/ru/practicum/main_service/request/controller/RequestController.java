package ru.practicum.main_service.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main_service.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main_service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main_service.request.dto.ParticipationRequestDto;
import ru.practicum.main_service.request.dto.param.CancelRequestParamDto;
import ru.practicum.main_service.request.dto.param.RequestParamDto;
import ru.practicum.main_service.request.dto.param.UpdateRequestStatusParamDto;
import ru.practicum.main_service.request.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class RequestController {
    private final RequestService requestService;

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests - получение заявок пользователя", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        log.info("POST /users/{}/requests?eventId={} - создание заявки", userId, eventId);
        RequestParamDto paramDto = new RequestParamDto(userId, eventId);
        return requestService.createRequest(paramDto);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel - отмена заявки", userId, requestId);
        CancelRequestParamDto paramDto = new CancelRequestParamDto(userId, requestId);
        return requestService.cancelRequest(paramDto);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId, @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests - получение заявок на событие", userId, eventId);
        RequestParamDto paramDto = new RequestParamDto(userId, eventId);
        return requestService.getEventRequests(paramDto);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId, @PathVariable Long eventId, @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}/requests - изменение статуса заявок", userId, eventId);
        UpdateRequestStatusParamDto paramDto = new UpdateRequestStatusParamDto(userId, eventId, updateRequest);
        return requestService.updateRequestStatus(paramDto);
    }
}