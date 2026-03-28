package ru.practicum.main_service.request.service;

import ru.practicum.main_service.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main_service.request.dto.ParticipationRequestDto;
import ru.practicum.main_service.request.dto.param.CancelRequestParamDto;
import ru.practicum.main_service.request.dto.param.RequestParamDto;
import ru.practicum.main_service.request.dto.param.UpdateRequestStatusParamDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(RequestParamDto paramDto);

    ParticipationRequestDto cancelRequest(CancelRequestParamDto paramDto);

    List<ParticipationRequestDto> getEventRequests(RequestParamDto paramDto);

    EventRequestStatusUpdateResult updateRequestStatus(UpdateRequestStatusParamDto paramDto);

}