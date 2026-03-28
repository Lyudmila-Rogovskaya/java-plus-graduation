package ru.practicum.stat_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.stat_dto.EndpointHitDto;
import ru.practicum.stat_server.model.EndpointHit;

@Mapper(componentModel = "spring")
public interface StatMapper {

    @Mapping(target = "timestamp", source = "timestamp", dateFormat = "yyyy-MM-dd HH:mm:ss")
    EndpointHit toEntity(EndpointHitDto dto);

    @Mapping(target = "timestamp", source = "timestamp", dateFormat = "yyyy-MM-dd HH:mm:ss")
    EndpointHitDto toDto(EndpointHit entity);
}