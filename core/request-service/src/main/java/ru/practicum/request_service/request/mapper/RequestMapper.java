package ru.practicum.request_service.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.request_service.request.dto.ParticipationRequestDto;
import ru.practicum.request_service.request.model.Request;
import ru.practicum.request_service.request.model.RequestStatus;

@Component
public class RequestMapper {

    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }

    public Request toEntity(Long eventId, Long requesterId) {
        return Request.builder()
                .eventId(eventId)
                .requesterId(requesterId)
                .status(RequestStatus.PENDING)
                .build();
    }

}
