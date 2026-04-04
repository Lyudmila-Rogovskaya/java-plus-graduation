package ru.practicum.event_service.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.event_service.event.dto.ModerationCommentDto;
import ru.practicum.event_service.event.model.ModerationComment;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ModerationCommentMapper {

    @Mapping(target = "eventId", source = "event.id")
    ModerationCommentDto toDto(ModerationComment comment);

    List<ModerationCommentDto> toDtoList(List<ModerationComment> comments);

}
