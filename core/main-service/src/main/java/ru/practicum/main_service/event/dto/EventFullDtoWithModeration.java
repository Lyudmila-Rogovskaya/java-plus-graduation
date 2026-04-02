package ru.practicum.main_service.event.dto;

import lombok.*;
import ru.practicum.main_service.category.dto.CategoryDto;
import ru.practicum.main_service.event.model.EventState;
import ru.practicum.main_service.event.model.Location;
import ru.practicum.main_service.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDtoWithModeration extends EventFullDto {

    private List<ModerationCommentDto> moderationComments;

    public EventFullDtoWithModeration(Long id, String annotation, CategoryDto category, Long confirmedRequests,
                                      LocalDateTime createdOn, String description, LocalDateTime eventDate,
                                      UserShortDto initiator, Location location, Boolean paid,
                                      Integer participantLimit, LocalDateTime publishedOn,
                                      Boolean requestModeration, EventState state, String title,
                                      Long views, List<ModerationCommentDto> moderationComments) {
        super(id, annotation, category, confirmedRequests, createdOn, description, eventDate,
                initiator, location, paid, participantLimit, publishedOn, requestModeration,
                state, title, views);
        this.moderationComments = moderationComments;
    }

    public static EventFullDtoWithModeration fromEventFullDto(EventFullDto eventFullDto,
                                                              List<ModerationCommentDto> moderationComments) {
        return new EventFullDtoWithModeration(
                eventFullDto.getId(),
                eventFullDto.getAnnotation(),
                eventFullDto.getCategory(),
                eventFullDto.getConfirmedRequests(),
                eventFullDto.getCreatedOn(),
                eventFullDto.getDescription(),
                eventFullDto.getEventDate(),
                eventFullDto.getInitiator(),
                eventFullDto.getLocation(),
                eventFullDto.getPaid(),
                eventFullDto.getParticipantLimit(),
                eventFullDto.getPublishedOn(),
                eventFullDto.getRequestModeration(),
                eventFullDto.getState(),
                eventFullDto.getTitle(),
                eventFullDto.getViews(),
                moderationComments
        );
    }

}
