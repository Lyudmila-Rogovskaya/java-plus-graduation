package ru.practicum.main_service.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.request.dto.ParticipationRequestDto;
import ru.practicum.main_service.request.model.Request;
import ru.practicum.main_service.request.model.RequestStatus;
import ru.practicum.main_service.user.model.User;

@Component
public class RequestMapper {

    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder().id(request.getId()).created(request.getCreated()).event(request.getEvent().getId()).requester(request.getRequester().getId()).status(request.getStatus()).build();
    }

    public Request toEntity(Event event, User requester) {
        return Request.builder().event(event).requester(requester).status(RequestStatus.PENDING).build();
    }
}