package ru.practicum.request_service.request.service;

import ru.practicum.request_service.request.dto.ConfirmedRequestsDto;
import ru.practicum.request_service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request_service.request.dto.ParticipationRequestDto;
import ru.practicum.request_service.request.dto.param.CancelRequestParamDto;
import ru.practicum.request_service.request.dto.param.RequestParamDto;
import ru.practicum.request_service.request.dto.param.UpdateRequestStatusParamDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(RequestParamDto paramDto);

    ParticipationRequestDto cancelRequest(CancelRequestParamDto paramDto);

    List<ParticipationRequestDto> getEventRequests(RequestParamDto paramDto);

    EventRequestStatusUpdateResult updateRequestStatus(UpdateRequestStatusParamDto paramDto);

    List<ConfirmedRequestsDto> getConfirmedRequestsCount(List<Long> eventIds);

    boolean hasUserVisitedEvent(Long userId, Long eventId);

}
