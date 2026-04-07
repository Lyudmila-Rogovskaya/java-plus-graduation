package ru.practicum.moderation_service.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.practicum.moderation_service.event.dto.ModerationCommentDto;
import ru.practicum.moderation_service.event.model.ModerationComment;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ModerationCommentMapper {

    ModerationCommentDto toDto(ModerationComment comment);

    List<ModerationCommentDto> toDtoList(List<ModerationComment> comments);

}
