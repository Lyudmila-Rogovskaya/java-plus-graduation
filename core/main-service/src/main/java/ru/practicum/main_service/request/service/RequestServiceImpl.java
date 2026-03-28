package ru.practicum.main_service.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.model.EventState;
import ru.practicum.main_service.event.repository.EventRepository;
import ru.practicum.main_service.exception.ConflictException;
import ru.practicum.main_service.exception.NotFoundException;
import ru.practicum.main_service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main_service.request.dto.ParticipationRequestDto;
import ru.practicum.main_service.request.dto.param.CancelRequestParamDto;
import ru.practicum.main_service.request.dto.param.RequestParamDto;
import ru.practicum.main_service.request.dto.param.UpdateRequestStatusParamDto;
import ru.practicum.main_service.request.mapper.RequestMapper;
import ru.practicum.main_service.request.model.Request;
import ru.practicum.main_service.request.model.RequestStatus;
import ru.practicum.main_service.request.repository.RequestRepository;
import ru.practicum.main_service.user.model.User;
import ru.practicum.main_service.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private static final String EVENT_NOT_FOUND = "Событие с ID %s не найдено";
    private static final String LIMIT_REACHED = "Достигнут лимит участников";

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение заявок пользователя с ID: {}", userId);
        return requestRepository.findByRequesterId(userId).stream().map(requestMapper::toDto).toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(RequestParamDto paramDto) {
        log.info("Создание заявки пользователя {} на событие {}", paramDto.getUserId(), paramDto.getEventId());

        User user = userRepository.findById(paramDto.getUserId()).orElseThrow(() -> new NotFoundException(String.format("Пользователь с ID %s не найден", paramDto.getEventId())));

        Event event = eventRepository.findById(paramDto.getEventId()).orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, paramDto.getEventId())));

        validateRequestCreation(paramDto.getUserId(), paramDto.getEventId(), event);

        Request request = createRequestEntity(event, user);
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
        log.info("Получение заявок на событие {} пользователя {}", paramDto.getEventId(), paramDto.getUserId());

        if (!eventRepository.existsByIdAndInitiatorId(paramDto.getEventId(), paramDto.getUserId())) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, paramDto.getEventId()));
        }

        return requestRepository.findByEventId(paramDto.getEventId()).stream().map(requestMapper::toDto).toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(UpdateRequestStatusParamDto paramDto) {
        log.info("Изменение статуса заявок на событие {} пользователем {}", paramDto.getEventId(), paramDto.getUserId());

        Event event = eventRepository.findByIdAndInitiatorId(paramDto.getEventId(), paramDto.getUserId()).orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, paramDto.getEventId())));

        List<Request> requests = requestRepository.findByIdIn(paramDto.getUpdateRequest().getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("Заявки не найдены");
        }

        if (shouldAutoConfirmRequests(event)) {
            return autoConfirmRequests(requests);
        }

        return processRequestsWithLimit(event, requests, paramDto.getUpdateRequest().getStatus());
    }

    private void validateRequestCreation(Long userId, Long eventId, Event event) {
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        if (isParticipantLimitReached(event, confirmedRequests)) {
            throw new ConflictException(LIMIT_REACHED);
        }
    }

    private Request createRequestEntity(Event event, User user) {
        Request request = requestMapper.toEntity(event, user);

        if (isRequestModerationNotRequired(event) || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            log.debug("Заявка автоматически подтверждена (отключена модерация или лимит 0)");
        } else {
            request.setStatus(RequestStatus.PENDING);
            log.debug("Заявка создана со статусом PENDING");
        }

        return request;
    }

    private boolean shouldAutoConfirmRequests(Event event) {
        return isRequestModerationNotRequired(event) || event.getParticipantLimit() == 0;
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

    private EventRequestStatusUpdateResult processRequestsWithLimit(Event event, List<Request> requests, RequestStatus targetStatus) {
        long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(event.getId());
        long availableSlots = event.getParticipantLimit() - confirmedRequests;

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
            throw new ConflictException("Можно изменять только заявки в статусе PENDING" + ". Заявка ID: " + request.getId() + " имеет статус: " + request.getStatus());
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

    private boolean isRequestModerationNotRequired(Event event) {
        return event.getRequestModeration() != null && !event.getRequestModeration();
    }

    private boolean isParticipantLimitReached(Event event, long confirmedCount) {
        if (event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
            return false;
        }
        return confirmedCount >= event.getParticipantLimit();
    }

}