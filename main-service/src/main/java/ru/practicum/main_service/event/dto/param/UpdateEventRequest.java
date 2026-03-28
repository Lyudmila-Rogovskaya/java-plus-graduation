package ru.practicum.main_service.event.dto.param;

import ru.practicum.main_service.event.model.Location;
import ru.practicum.main_service.event.model.StateAction;

import java.time.LocalDateTime;

public interface UpdateEventRequest {
    String getAnnotation();

    Long getCategory();

    String getDescription();

    LocalDateTime getEventDate();

    Location getLocation();

    Boolean getPaid();

    Integer getParticipantLimit();

    Boolean getRequestModeration();

    String getTitle();

    StateAction getStateAction();

}
