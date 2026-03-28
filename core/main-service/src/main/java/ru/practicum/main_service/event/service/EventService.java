package ru.practicum.main_service.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.main_service.event.dto.EventFullDto;
import ru.practicum.main_service.event.dto.EventFullDtoWithModeration;
import ru.practicum.main_service.event.dto.EventShortDto;
import ru.practicum.main_service.event.dto.NewEventDto;
import ru.practicum.main_service.event.dto.param.*;

import java.util.List;

public interface EventService {
    List<EventShortDto> getEventsByUser(EventsByUserParams params);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getEventByUser(EventByUserRequest request);

    EventFullDto updateEventByUser(EventByUserRequest request, UpdateEventUserRequest updateEvent);

    List<EventFullDto> getEventsByAdmin(EventsByAdminParams params);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEvent);

    List<EventShortDto> getEventsPublic(EventsPublicParams params);

    EventFullDto getEventPublic(Long eventId, HttpServletRequest request);

    EventFullDtoWithModeration updateEventByAdminWithComment(Long eventId,
                                                             UpdateEventAdminRequestWithComment updateRequest);

    List<EventFullDtoWithModeration> getEventsForModeration(Integer from, Integer size);

}
