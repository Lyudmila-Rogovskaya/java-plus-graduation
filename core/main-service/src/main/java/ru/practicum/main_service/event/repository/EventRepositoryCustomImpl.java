package ru.practicum.main_service.event.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.model.EventState;
import ru.practicum.main_service.request.model.Request;
import ru.practicum.main_service.request.model.RequestStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<Event> findEventsByAdmin(List<Long> users, List<EventState> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable pageable) {
        log.debug("Criteria API: админский поиск. users={}, states={}, categories={}, range=[{}, {}]", users, states, categories, rangeStart, rangeEnd);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);
        log.trace("Применен fetch join для категории и инициатора");
        event.fetch("category", JoinType.LEFT);
        event.fetch("initiator", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        if (users != null && !users.isEmpty()) {
            predicates.add(event.get("initiator").get("id").in(users));
            log.trace("Добавлен фильтр по пользователям: {}", users);
        }

        if (states != null && !states.isEmpty()) {
            predicates.add(event.get("state").in(states));
            log.trace("Добавлен фильтр по состояниям: {}", states);
        }

        if (categories != null && !categories.isEmpty()) {
            predicates.add(event.get("category").get("id").in(categories));
            log.trace("Добавлен фильтр по категориям: {}", categories);
        }

        LocalDateTime startDate = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime endDate = rangeEnd != null ? rangeEnd : LocalDateTime.now().plusYears(100);
        predicates.add(cb.between(event.get("eventDate"), startDate, endDate));
        log.trace("Диапазон дат: с {} по {}", startDate, endDate);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(event.get("id")));
        log.trace("Сформировано {} предикатов, сортировка по ID", predicates.size());
        TypedQuery<Event> query = entityManager.createQuery(cq);
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
            log.trace("Применена пагинация: offset={}, limit={}", pageable.getOffset(), pageable.getPageSize());
        }

        List<Event> result = query.getResultList();
        log.debug("Criteria API возвращает {} событий (админский поиск)", result.size());
        return result;
    }

    @Override
    public List<Event> findEventsPublic(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, Pageable pageable) {
        log.debug("Criteria API: публичный поиск. text={}, categories={}, paid={}, onlyAvailable={}", text, categories, paid, onlyAvailable);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);

        event.fetch("category", JoinType.LEFT);
        event.fetch("initiator", JoinType.LEFT);
        log.trace("Применен fetch join для оптимизации запросов");
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(event.get("state"), EventState.PUBLISHED));
        log.trace("Обязательный фильтр: state=PUBLISHED");

        if (text != null && !text.trim().isEmpty()) {
            String searchPattern = "%" + text.toLowerCase() + "%";
            Predicate annotationPredicate = cb.like(cb.lower(event.get("annotation")), searchPattern);
            Predicate descriptionPredicate = cb.like(cb.lower(event.get("description")), searchPattern);
            predicates.add(cb.or(annotationPredicate, descriptionPredicate));
            log.trace("Добавлен текстовый поиск по аннотации/описанию: '{}'", text);
        }

        if (categories != null && !categories.isEmpty()) {
            predicates.add(event.get("category").get("id").in(categories));
            log.trace("Добавлен фильтр по категориям : {}", categories);
        }

        if (paid != null) {
            predicates.add(cb.equal(event.get("paid"), paid));
            log.trace("Добавлен фильтр по оплате: {}", paid);
        }

        LocalDateTime startDate = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime endDate = rangeEnd != null ? rangeEnd : LocalDateTime.now().plusYears(100);
        predicates.add(cb.between(event.get("eventDate"), startDate, endDate));
        log.trace("Диапазон дат : с {} по {}", startDate, endDate);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            log.trace("Добавлен сложный фильтр onlyAvailable с подзапросом");
            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<Request> request = subquery.from(Request.class);
            subquery.select(cb.count(request.get("id"))).where(cb.and(cb.equal(request.get("event").get("id"), event.get("id")), cb.equal(request.get("status"), RequestStatus.CONFIRMED)));

            Predicate noLimit = cb.equal(event.get("participantLimit"), 0);
            Predicate hasLimitAndAvailable = cb.and(cb.greaterThan(event.get("participantLimit"), 0), cb.lessThan(subquery, event.get("participantLimit")));

            predicates.add(cb.or(noLimit, hasLimitAndAvailable));
            log.trace("Условие onlyAvailable: participantLimit=0 OR confirmedRequests < participantLimit");
        }

        cq.where(predicates.toArray(new Predicate[0]));
        log.trace("Всего предикатов: {}", predicates.size());

        if (pageable != null && pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.getProperty().equals("eventDate")) {
                    if (order.isAscending()) {
                        orders.add(cb.asc(event.get("eventDate")));
                    } else {
                        orders.add(cb.desc(event.get("eventDate")));
                    }
                    log.trace("Добавлена сортировка по eventDate: {}", order.isAscending() ? "ASC" : "DESC");
                }
            });
            if (!orders.isEmpty()) {
                cq.orderBy(orders);
            } else {
                cq.orderBy(cb.asc(event.get("eventDate")));
                log.trace("Сортировка по умолчанию: eventDate ASC");
            }
        } else {
            cq.orderBy(cb.asc(event.get("eventDate")));
            log.trace("Сортировка по умолчанию : eventDate ASC");
        }

        TypedQuery<Event> query = entityManager.createQuery(cq);
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
            log.trace("Применена пагинация : offset={}, limit={}", pageable.getOffset(), pageable.getPageSize());
        }

        List<Event> result = query.getResultList();
        log.debug("Criteria API возвращает {} событий (публичный поиск)", result.size());
        return result;
    }
}
