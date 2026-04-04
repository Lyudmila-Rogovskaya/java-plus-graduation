package ru.practicum.request_service.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.request_service.client.EventClient;
import ru.practicum.request_service.client.UserClient;
import ru.practicum.request_service.dto.EventValidationDto;
import ru.practicum.request_service.exception.ConflictException;
import ru.practicum.request_service.exception.NotFoundException;
import ru.practicum.request_service.request.dto.ConfirmedRequestsDto;
import ru.practicum.request_service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request_service.request.dto.ParticipationRequestDto;
import ru.practicum.request_service.request.dto.param.CancelRequestParamDto;
import ru.practicum.request_service.request.dto.param.RequestParamDto;
import ru.practicum.request_service.request.dto.param.UpdateRequestStatusParamDto;
import ru.practicum.request_service.request.mapper.RequestMapper;
import ru.practicum.request_service.request.model.Request;
import ru.practicum.request_service.request.model.RequestStatus;
import ru.practicum.request_service.request.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private static final String LIMIT_REACHED = "Достигнут лимит участников";

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение заявок пользователя с ID: {}", userId);
        try {
            userClient.getUser(userId);
        } catch (Exception e) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(RequestParamDto paramDto) {
        Long userId = paramDto.getUserId();
        Long eventId = paramDto.getEventId();
        log.info("Создание заявки пользователя {} на событие {}", userId, eventId);

        try {
            userClient.getUser(userId);
        } catch (Exception e) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        EventValidationDto eventValidation;
        try {
            eventValidation = eventClient.validateEvent(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено");
        }

        validateRequestCreation(userId, eventId, eventValidation);

        Request request = requestMapper.toEntity(eventId, userId);

        if (isRequestModerationNotRequired(eventValidation) || eventValidation.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            log.debug("Заявка автоматически подтверждена (отключена модерация или лимит 0)");
        } else {
            request.setStatus(RequestStatus.PENDING);
            log.debug("Заявка создана со статусом PENDING");
        }

        Request savedRequest = requestRepository.save(request);

        log.info("Заявка создана с ID: {}", savedRequest.getId());
        return requestMapper.toDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(CancelRequestParamDto paramDto) {
        log.info("Отмена заявки {} пользователем {}", paramDto.getRequestId(), paramDto.getUserId());

        Request request = requestRepository.findByIdAndRequesterId(paramDto.getRequestId(), paramDto.getUserId()).orElseThrow(() -> new NotFoundException(String.format("Заявка с ID %s не найдена", paramDto.getRequestId())));

        if (request.getStatus().equals(RequestStatus.CANCELED)) {
            throw new ConflictException("Заявка уже отменена");
        }

        request.setStatus(RequestStatus.CANCELED);
        Request updated = requestRepository.save(request);

        log.info("Заявка {} отменена пользователем {}", paramDto.getRequestId(), paramDto.getUserId());
        return requestMapper.toDto(updated);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(RequestParamDto paramDto) {
        Long userId = paramDto.getUserId();
        Long eventId = paramDto.getEventId();
        log.info("Получение заявок на событие {} пользователя {}", eventId, userId);

        EventValidationDto eventValidation;
        try {
            eventValidation = eventClient.validateEvent(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено");
        }

        if (!eventValidation.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено или вы не являетесь его инициатором");
        }

        return requestRepository.findByEventId(eventId).stream().map(requestMapper::toDto).toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(UpdateRequestStatusParamDto paramDto) {
        Long userId = paramDto.getUserId();
        Long eventId = paramDto.getEventId();
        log.info("Изменение статуса заявок на событие {} пользователем {}", eventId, userId);

        EventValidationDto eventValidation;
        try {
            eventValidation = eventClient.validateEvent(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено");
        }

        if (!eventValidation.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено или вы не являетесь его инициатором");
        }

        List<Request> requests = requestRepository.findByIdIn(paramDto.getUpdateRequest().getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("Заявки не найдены");
        }

        if (shouldAutoConfirmRequests(eventValidation)) {
            return autoConfirmRequests(requests);
        }

        return processRequestsWithLimit(eventValidation, requests, paramDto.getUpdateRequest().getStatus());
    }

    private void validateRequestCreation(Long userId, Long eventId, EventValidationDto eventValidation) {
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (eventValidation.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (!eventValidation.getPublished()) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (isParticipantLimitReached(eventValidation, confirmedRequests)) {
            throw new ConflictException(LIMIT_REACHED);
        }
    }

    private boolean isRequestModerationNotRequired(EventValidationDto eventValidation) {
        return eventValidation.getRequestModeration() != null && !eventValidation.getRequestModeration();
    }

    private boolean isParticipantLimitReached(EventValidationDto eventValidation, long confirmedCount) {
        Integer limit = eventValidation.getParticipantLimit();
        return limit != null && limit > 0 && confirmedCount >= limit;
    }

    private boolean shouldAutoConfirmRequests(EventValidationDto eventValidation) {
        return isRequestModerationNotRequired(eventValidation) || eventValidation.getParticipantLimit() == 0;
    }

    private EventRequestStatusUpdateResult autoConfirmRequests(List<Request> requests) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();

        for (Request request : requests) {
            if (request.getStatus().equals(RequestStatus.PENDING)) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(requestMapper.toDto(request));
            }
        }

        requestRepository.saveAll(requests);
        log.info("Все заявки автоматически подтверждены (отключена модерация или лимит 0)");

        return new EventRequestStatusUpdateResult(confirmed, List.of());
    }

    private EventRequestStatusUpdateResult processRequestsWithLimit(EventValidationDto eventValidation,
                                                                    List<Request> requests,
                                                                    RequestStatus targetStatus) {
        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventValidation.getEventId(), RequestStatus.CONFIRMED);
        long availableSlots = eventValidation.getParticipantLimit() - confirmedRequests;

        if (targetStatus == RequestStatus.CONFIRMED && availableSlots <= 0) {
            throw new ConflictException(LIMIT_REACHED);
        }

        return processEachRequest(requests, targetStatus, availableSlots);
    }

    private EventRequestStatusUpdateResult processEachRequest(List<Request> requests, RequestStatus targetStatus, long availableSlots) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Request request : requests) {
            validateRequestStatus(request);
            availableSlots = updateRequestStatus(request, targetStatus, availableSlots, confirmed, rejected);
        }

        requestRepository.saveAll(requests);
        log.info("Статус заявок обновлён: подтверждено - {}, отклонено - {}", confirmed.size(), rejected.size());

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private void validateRequestStatus(Request request) {
        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new ConflictException("Можно изменять только заявки в статусе PENDING. " +
                    "Заявка ID: " + request.getId() + " имеет статус: " + request.getStatus());
        }
    }

    private long updateRequestStatus(Request request, RequestStatus targetStatus, long availableSlots, List<ParticipationRequestDto> confirmed, List<ParticipationRequestDto> rejected) {
        if (targetStatus == RequestStatus.CONFIRMED) {
            if (availableSlots > 0) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(requestMapper.toDto(request));
                availableSlots--;
                log.debug("Заявка {} подтверждена", request.getId());
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(requestMapper.toDto(request));
                log.debug("Заявка {} отклонена (лимит исчерпан)", request.getId());
            }
        } else if (targetStatus == RequestStatus.REJECTED) {
            request.setStatus(RequestStatus.REJECTED);
            rejected.add(requestMapper.toDto(request));
            log.debug("Заявка {} отклонена", request.getId());
        }

        return availableSlots;
    }

    @Override
    public List<ConfirmedRequestsDto> getConfirmedRequestsCount(List<Long> eventIds) {
        List<Object[]> results = requestRepository.countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED);
        return results.stream()
                .map(row -> new ConfirmedRequestsDto((Long) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

}
