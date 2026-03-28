package ru.practicum.main_service.event.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.main_service.category.mapper.CategoryMapper;
import ru.practicum.main_service.category.model.Category;
import ru.practicum.main_service.category.service.CategoryService;
import ru.practicum.main_service.event.dto.EventFullDto;
import ru.practicum.main_service.event.dto.EventShortDto;
import ru.practicum.main_service.event.dto.NewEventDto;
import ru.practicum.main_service.event.dto.param.UpdateEventRequest;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.model.EventState;
import ru.practicum.main_service.user.mapper.UserMapper;
import ru.practicum.main_service.user.model.User;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapper.class, UserMapper.class}, imports = {LocalDateTime.class, EventState.class})
public abstract class EventMapper {

    @Autowired
    protected CategoryService categoryService;

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "annotation", source = "annotation")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "eventDate", source = "eventDate")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "paid", source = "paid")
    @Mapping(target = "participantLimit", source = "participantLimit")
    @Mapping(target = "requestModeration", source = "requestModeration")
    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryIdToCategory")
    public abstract void updateEventFromRequest(UpdateEventRequest request, @MappingTarget Event event);

    @Named("mapCategoryIdToCategory")
    protected Category mapCategoryIdToCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryService.getEntityById(categoryId);
    }

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    public abstract EventFullDto toEventFullDto(Event event);

    public EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views) {
        EventFullDto dto = toEventFullDto(event);
        if (confirmedRequests != null) {
            dto.setConfirmedRequests(confirmedRequests);
        }
        if (views != null) {
            dto.setViews(views);
        }
        return dto;
    }

    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    public abstract EventShortDto toEventShortDto(Event event);

    public EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views) {
        EventShortDto dto = toEventShortDto(event);
        if (confirmedRequests != null) {
            dto.setConfirmedRequests(confirmedRequests);
        }
        if (views != null) {
            dto.setViews(views);
        }
        return dto;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "publishedOn", ignore = true)
    public abstract Event toNewEvent(NewEventDto newEventDto, Category category, User initiator);

}
