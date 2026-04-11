package ru.practicum.event_service.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event_service.category.model.Category;
import ru.practicum.event_service.category.service.CategoryService;
import ru.practicum.event_service.client.ModerationCommentClient;
import ru.practicum.event_service.client.RequestClient;
import ru.practicum.event_service.client.UserClient;
import ru.practicum.event_service.client.dto.ConfirmedRequestsDto;
import ru.practicum.event_service.client.dto.CreateCommentRequest;
import ru.practicum.event_service.client.dto.UserDto;
import ru.practicum.event_service.client.dto.UserShortDto;
import ru.practicum.event_service.event.dto.*;
import ru.practicum.event_service.event.dto.param.*;
import ru.practicum.event_service.event.mapper.EventMapper;
import ru.practicum.event_service.event.model.Event;
import ru.practicum.event_service.event.model.EventState;
import ru.practicum.event_service.event.model.StateAction;
import ru.practicum.event_service.event.repository.EventRepository;
import ru.practicum.event_service.exception.ConflictException;
import ru.practicum.event_service.exception.NotFoundException;
import ru.practicum.event_service.exception.ValidationException;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.stats_client.AnalyzerGrpcClient;
import ru.practicum.stats_client.CollectorGrpcClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final EventMapper eventMapper;
    private final RequestClient requestClient;
    private final ModerationCommentClient moderationCommentClient;
    private final UserClient userClient;
    private final CollectorGrpcClient collectorGrpcClient;
    private final AnalyzerGrpcClient analyzerGrpcClient;

    @Value("${event.moderation.page-size:10}")
    private int defaultModerationPageSize;

    @Value("${event.moderation.default-from:0}")
    private int defaultModerationFrom;

    private UserShortDto getUserShortDto(Long userId) {
        UserDto userDto = userClient.getUser(userId);
        return new UserShortDto(userDto.getId(), userDto.getName());
    }

    private Map<Long, Long> getConfirmedRequestsBatch(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        log.trace("Получение подтвержденных запросов для {} событий", events.size());
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<ConfirmedRequestsDto> results = requestClient.getConfirmedRequests(eventIds);
        log.trace("Получено {} результатов о подтвержденных запросах", results.size());
        return results.stream()
                .collect(Collectors.toMap(ConfirmedRequestsDto::getEventId, ConfirmedRequestsDto::getCount));
    }

    private Long getEventRequests(Event event) {
        List<ConfirmedRequestsDto> result = requestClient.getConfirmedRequests(List.of(event.getId()));
        return result.isEmpty() ? 0L : result.get(0).getCount();
    }

    private Double getEventRating(Event event) {
        return event.getRating() != null ? event.getRating() : 0.0;
    }

    private UserDto getUserById(Long userId) {
        return userClient.getUser(userId);
    }

    private Category getCategoryById(Long categoryId) {
        return categoryService.getEntityById(categoryId);
    }

    private void updateEventFields(Event event, UpdateEventRequest updateEvent) {
        eventMapper.updateEventFromRequest(updateEvent, event);
    }

    private void timeRangeValidation(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);
        if (rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Начальная дата не может быть позже конечной");
        }
    }

    @Override
    public List<EventShortDto> getEventsByUser(EventsByUserParams params) {
        log.debug("Получение событий пользователя: userId={}, from={}, size={}",
                params.getUserId(), params.getFrom(), params.getSize());
        Long userId = params.getUserId();
        getUserById(userId);
        int from = params.getFrom();
        int size = params.getSize();
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        log.trace("Пагинация: offset={}, limit={}", pageable.getOffset(), pageable.getPageSize());
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        if (events.isEmpty()) {
            log.trace("Пользователь userId={} не имеет событий", userId);
            return Collections.emptyList();
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(events);
        log.trace("Статистика подтвержденных запросов собрана: {} записей", confirmedRequestsMap.size());

        UserShortDto initiator = getUserShortDto(userId);

        List<EventShortDto> result = events.stream().map(event -> {
            Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Double rating = getEventRating(event);
            log.trace("Событие пользователя id={}: confirmedRequests={}, rating={}",
                    event.getId(), confirmed, rating);
            EventShortDto dto = eventMapper.toEventShortDto(event, confirmed, rating);
            dto.setInitiator(initiator);
            return dto;
        }).collect(Collectors.toList());
        log.debug("Возвращено {} EventShortDto для пользователя userId={}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        getUserById(userId);
        Category category = getCategoryById(newEventDto.getCategory());

        LocalDateTime now = LocalDateTime.now();
        if (newEventDto.getEventDate().isBefore(now)) {
            throw new ValidationException("Дата события не может быть в прошлом");
        }
        if (newEventDto.getEventDate().isBefore(now.plusHours(2))) {
            throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toNewEvent(newEventDto, category, userId);
        event.setRating(0.0);
        Event savedEvent = eventRepository.save(event);
        log.info("Создано новое событие с id: {}", savedEvent.getId());

        EventFullDto dto = eventMapper.toEventFullDto(savedEvent);
        dto.setInitiator(getUserShortDto(userId));
        return dto;
    }

    @Override
    public EventFullDto getEventByUser(EventByUserRequest request) {
        Long userId = request.getUserId();
        Long eventId = request.getEventId();
        getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() ->
                new NotFoundException("Событие с id=" + eventId + " не найдено"));
        Long eventRequests = getEventRequests(event);
        Double rating = getEventRating(event);
        EventFullDto dto = eventMapper.toEventFullDto(event, eventRequests, rating);
        dto.setInitiator(getUserShortDto(userId));
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(EventByUserRequest request, UpdateEventUserRequest updateEvent) {
        Long userId = request.getUserId();
        Long eventId = request.getEventId();
        getUserById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не принадлежит пользователю");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }

        if (updateEvent.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (updateEvent.getEventDate().isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }

            if (updateEvent.getEventDate().isBefore(now.plusHours(2))) {
                throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
        }

        updateEventFields(event, updateEvent);
        StateAction state = updateEvent.getStateAction();
        if (state != null) {
            if (!state.isUserStateAction()) {
                throw new ValidationException("Передано некорректное действие");
            }
            switch (state) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    if (event.getState() == EventState.CANCELED) {
                        try {
                            moderationCommentClient.deleteCommentsByEventId(eventId);
                        } catch (Exception e) {
                            log.error("Не удалось удалить комментарии модерации для события {}: {}", eventId, e.getMessage());
                        }
                    }
                    event.setState(EventState.PENDING);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие с id: {}", eventId);
        Long eventRequests = getEventRequests(updatedEvent);
        Double rating = getEventRating(updatedEvent);
        EventFullDto dto = eventMapper.toEventFullDto(updatedEvent, eventRequests, rating);
        dto.setInitiator(getUserShortDto(userId));
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEvent) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (updateEvent.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (updateEvent.getEventDate().isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }

            if (updateEvent.getEventDate().isBefore(now.plusHours(1))) {
                throw new ConflictException("Дата события должна быть не ранее чем через 1 час от текущего момента");
            }
        }

        updateEventFields(event, updateEvent);
        StateAction state = updateEvent.getStateAction();
        if (state != null) {
            switch (state) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Событие можно публиковать только если оно в состоянии ожидания");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Администратором обновлено событие с id: {}", eventId);
        Long eventRequests = getEventRequests(updatedEvent);
        Double rating = getEventRating(updatedEvent);
        EventFullDto dto = eventMapper.toEventFullDto(updatedEvent, eventRequests, rating);
        dto.setInitiator(getUserShortDto(event.getInitiatorId()));
        return dto;
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(EventsByAdminParams params) {
        log.debug("Админский поиск событий: users={}, states={}, categories={}, from={}, size={}",
                params.getUsers(), params.getStates(), params.getCategories(), params.getFrom(), params.getSize());
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        timeRangeValidation(rangeStart, rangeEnd);
        int from = params.getFrom();
        int size = params.getSize();
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Long> users = params.getUsers();
        List<EventState> states = params.getStates();
        List<Long> categories = params.getCategories();
        log.trace("Критерии поиска: users={}, states={}, categories={}, диапазон=[{}, {}]",
                users, states, categories, rangeStart, rangeEnd);

        List<Event> events = eventRepository.findEventsByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        log.debug("Найдено {} событий для администратора", events.size());
        if (events.isEmpty()) {
            log.trace("События не найдены по указанным критериям");
            return Collections.emptyList();
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(events);
        log.trace("Статистика подтвержденных запросов собрана: {} записей", confirmedRequestsMap.size());

        Set<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        Map<Long, UserShortDto> userCache = userIds.stream()
                .collect(Collectors.toMap(uid -> uid, this::getUserShortDto));

        return events.stream().map(event -> {
            Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Double rating = getEventRating(event);
            EventFullDto dto = eventMapper.toEventFullDto(event, confirmed, rating);
            dto.setInitiator(userCache.get(event.getInitiatorId()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<EventShortDto> getEventsPublic(EventsPublicParams params) {
        log.debug("Публичный поиск событий: sort={}, onlyAvailable={}, categories={}",
                params.getSort(), params.getOnlyAvailable(), params.getCategories());
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        timeRangeValidation(rangeStart, rangeEnd);
        int from = params.getFrom();
        int size = params.getSize();
        String text = params.getText();
        Boolean paid = params.getPaid();
        Boolean onlyAvailable = params.getOnlyAvailable();
        List<Long> categories = params.getCategories();
        boolean sortedByRating = "rating".equalsIgnoreCase(params.getSort());
        List<EventShortDto> result;
        if (sortedByRating) {
            log.debug("Сортировка по рейтингу (in-memory)");
            result = findSortedByRating(from, size, text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
        } else {
            log.debug("Сортировка по дате (DB-level)");
            result = findSortedByDate(from, size, text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
        }
        log.debug("Возвращено {} событий", result.size());
        return result;
    }

    private List<EventShortDto> findSortedByDate(int from, int size, String text, List<Long> categories,
                                                 Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                 Boolean onlyAvailable) {
        Sort sorting = Sort.by("eventDate").ascending();
        Pageable pageable = PageRequest.of(from / size, size, sorting);
        log.trace("Поиск с пагинацией: offset={}, limit={}", pageable.getOffset(), pageable.getPageSize());
        List<Event> events = eventRepository.findEventsPublic(text, categories, paid, rangeStart, rangeEnd, pageable);
        log.trace("Найдено {} событий (сортировка по дате)", events.size());
        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = filterOnlyAvailable(events);
            log.trace("После фильтрации onlyAvailable осталось {} событий", events.size());
        }
        return enrichEventShortDtoWithUsers(events);
    }

    private List<EventShortDto> findSortedByRating(int from, int size, String text, List<Long> categories,
                                                   Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                   Boolean onlyAvailable) {
        log.trace("Поиск без пагинации для сортировки по рейтингу");
        List<Event> events = eventRepository.findEventsPublic(text, categories, paid, rangeStart, rangeEnd, null);
        log.trace("Найдено {} событий для сортировки по рейтингу", events.size());

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = filterOnlyAvailable(events);
            log.trace("После фильтрации onlyAvailable осталось {} событий", events.size());
        }

        List<EventShortDto> result = enrichEventShortDtoWithUsers(events);
        if (result.size() > 100) {
            log.debug("Сортировка в памяти для {} событий (может быть затратно)", result.size());
        }
        result.sort(Comparator.comparing(EventShortDto::getRating).reversed());
        int toIndex = Math.min(from + size, result.size());
        if (from >= result.size()) {
            log.trace("Запрошенный offset превышает количество найденных событий");
            return Collections.emptyList();
        }
        return result.subList(from, toIndex);
    }

    private List<EventShortDto> enrichEventShortDtoWithUsers(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();
        if (events.size() > 20) {
            log.trace("Запрос статистики для {} событий (batch)", events.size());
        }
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(events);

        Set<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        Map<Long, UserShortDto> userCache = userIds.stream()
                .collect(Collectors.toMap(uid -> uid, this::getUserShortDto));

        List<EventShortDto> result = events.stream().map(event -> {
            Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Double rating = getEventRating(event);
            EventShortDto dto = eventMapper.toEventShortDto(event, confirmed, rating);
            dto.setInitiator(userCache.get(event.getInitiatorId()));
            return dto;
        }).collect(Collectors.toList());
        log.trace("Создано {} DTO", result.size());
        return result;
    }

    @Override
    public EventFullDto getEventPublic(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не опубликовано");
        }

        String userIdHeader = request.getHeader("X-EWM-USER-ID");
        if (userIdHeader != null) {
            try {
                long userId = Long.parseLong(userIdHeader);
                collectorGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
                log.debug("Отправлено действие VIEW для пользователя {} по событию {}", userId, eventId);
            } catch (NumberFormatException e) {
                log.warn("Некорректный заголовок X-EWM-USER-ID: {}", userIdHeader);
            }
        }

        Long eventRequests = getEventRequests(event);
        Double rating = getEventRating(event);
        EventFullDto dto = eventMapper.toEventFullDto(event, eventRequests, rating);
        dto.setInitiator(getUserShortDto(event.getInitiatorId()));
        return dto;
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        log.info("Получение рекомендаций для пользователя {}", userId);
        try (Stream<RecommendedEventProto> stream = analyzerGrpcClient.getUserPredictions(userId, maxResults)) {
            List<RecommendedEventProto> protoList = stream.toList();
            if (protoList.isEmpty()) {
                log.debug("Для пользователя {} не найдено рекомендаций", userId);
                return Collections.emptyList();
            }
            List<Long> eventIds = protoList.stream().map(RecommendedEventProto::getEventId).toList();
            Map<Long, Double> scoreMap = protoList.stream()
                    .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));

            List<Event> events = eventRepository.findAllById(eventIds);
            if (events.isEmpty()) {
                log.debug("События по рекомендациям для пользователя {} не найдены", userId);
                return Collections.emptyList();
            }

            List<ConfirmedRequestsDto> confirmed = requestClient.getConfirmedRequests(eventIds);
            Map<Long, Long> confirmedMap = confirmed.stream()
                    .collect(Collectors.toMap(ConfirmedRequestsDto::getEventId, ConfirmedRequestsDto::getCount));

            Set<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
            Map<Long, UserShortDto> userCache = userIds.stream()
                    .collect(Collectors.toMap(uid -> uid, this::getUserShortDto));

            return events.stream().map(event -> {
                        Long conf = confirmedMap.getOrDefault(event.getId(), 0L);
                        Double rating = scoreMap.getOrDefault(event.getId(), event.getRating());
                        EventShortDto dto = eventMapper.toEventShortDto(event, conf, rating);
                        dto.setInitiator(userCache.get(event.getInitiatorId()));
                        return dto;
                    }).sorted(Comparator.comparing(EventShortDto::getRating).reversed())
                    .limit(maxResults)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций для пользователя {}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        log.info("Пользователь {} ставит лайк событию {}", userId, eventId);
        if (!requestClient.hasUserVisitedEvent(userId, eventId)) {
            throw new ValidationException("Пользователь должен посетить событие, прежде чем ставить лайк");
        }
        collectorGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    @Transactional
    public EventFullDtoWithModeration updateEventByAdminWithComment(Long eventId,
                                                                    UpdateEventAdminRequestWithComment updateRequest) {
        log.info("Обновление события с id: {} администратором с комментарием", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        UpdateEventAdminRequest updateEvent = updateRequest.getUpdateEvent();
        String moderationComment = updateRequest.getModerationComment();

        if (updateEvent.getEventDate() != null) {
            LocalDateTime now = LocalDateTime.now();

            if (updateEvent.getEventDate().isBefore(now)) {
                throw new ValidationException("Дата события не может быть в прошлом");
            }

            if (updateEvent.getEventDate().isBefore(now.plusHours(1))) {
                throw new ConflictException("Дата события должна быть не ранее чем через 1 час от текущего момента");
            }
        }

        updateEventFields(event, updateEvent);

        StateAction state = updateEvent.getStateAction();
        Long adminId = 1L;

        if (state != null) {
            switch (state) {
                case PUBLISH_EVENT:
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Событие можно публиковать только если оно в состоянии ожидания");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());

                    if (moderationComment != null && !moderationComment.trim().isEmpty()) {
                        CreateCommentRequest request = new CreateCommentRequest(eventId, adminId, moderationComment);
                        try {
                            moderationCommentClient.createComment(request);
                        } catch (Exception e) {
                            log.error("Не удалось создать комментарий модерации для события {}: {}", eventId, e.getMessage());
                        }
                    }
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отклонить опубликованное событие");
                    }
                    event.setState(EventState.CANCELED);

                    if (moderationComment == null || moderationComment.trim().isEmpty()) {
                        throw new ValidationException("При отклонении события необходимо указать причину");
                    }
                    CreateCommentRequest request = new CreateCommentRequest(eventId, adminId, moderationComment.trim());
                    try {
                        moderationCommentClient.createComment(request);
                    } catch (Exception e) {
                        log.error("Не удалось создать комментарий модерации для события {}: {}", eventId, e.getMessage());
                    }
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Администратором обновлено событие с id: {} с комментарием модерации", eventId);

        List<ModerationCommentDto> comments = Collections.emptyList();
        try {
            comments = moderationCommentClient.getCommentsByEventIds(List.of(eventId));
        } catch (Exception e) {
            log.error("Не удалось получить комментарии модерации для события {}: {}", eventId, e.getMessage());
        }

        Long eventRequests = getEventRequests(updatedEvent);
        Double rating = getEventRating(updatedEvent);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(updatedEvent, eventRequests, rating);
        eventFullDto.setInitiator(getUserShortDto(event.getInitiatorId()));
        return EventFullDtoWithModeration.fromEventFullDto(eventFullDto, comments);
    }

    @Override
    public List<EventFullDtoWithModeration> getEventsForModeration(Integer from, Integer size) {
        log.info("Получение событий для модерации, from={}, size={}", from, size);

        if (from == null) from = defaultModerationFrom;
        if (size == null) size = defaultModerationPageSize;

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").ascending());

        List<Event> events = eventRepository.findByState(EventState.PENDING, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(events);

        Map<Long, List<ModerationCommentDto>> commentsMap = getCommentsBatch(eventIds);

        Set<Long> userIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        Map<Long, UserShortDto> userCache = userIds.stream()
                .collect(Collectors.toMap(uid -> uid, this::getUserShortDto));

        return events.stream().map(event -> {
            Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            Double rating = getEventRating(event);
            EventFullDto eventFullDto = eventMapper.toEventFullDto(event, confirmed, rating);
            eventFullDto.setInitiator(userCache.get(event.getInitiatorId()));
            List<ModerationCommentDto> comments = commentsMap.getOrDefault(event.getId(), Collections.emptyList());
            return EventFullDtoWithModeration.fromEventFullDto(eventFullDto, comments);
        }).collect(Collectors.toList());
    }

    private Map<Long, List<ModerationCommentDto>> getCommentsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<ModerationCommentDto> allComments = moderationCommentClient.getCommentsByEventIds(eventIds);
            return allComments.stream()
                    .collect(Collectors.groupingBy(
                            ModerationCommentDto::getEventId,
                            Collectors.toList()
                    ));
        } catch (Exception e) {
            log.error("Не удалось получить комментарии модерации для eventIds {}: {}", eventIds, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public EventValidationDto validateEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + eventId));
        return EventValidationDto.builder()
                .eventId(event.getId())
                .published(event.getState() == EventState.PUBLISHED)
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .initiatorId(event.getInitiatorId())
                .title(event.getTitle())
                .build();
    }

    @Override
    public EventDto getEventDto(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + eventId));
        return EventDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .title(event.getTitle())
                .build();
    }

    private List<Event> filterOnlyAvailable(List<Event> events) {
        if (events.isEmpty()) return events;

        Map<Long, Long> confirmedRequests = getConfirmedRequestsBatch(events);

        return events.stream()
                .filter(event -> {
                    int limit = event.getParticipantLimit();
                    if (limit == 0) return true;
                    long confirmed = confirmedRequests.getOrDefault(event.getId(), 0L);
                    return confirmed < limit;
                })
                .collect(Collectors.toList());
    }

}
