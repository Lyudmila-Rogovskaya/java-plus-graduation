package ru.practicum.request_service.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.request_service.request.model.Request;
import ru.practicum.request_service.request.model.RequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequesterId(Long userId);

    List<Request> findByEventId(Long eventId);

    List<Request> findByIdIn(List<Long> ids);

    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long userId);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT r.eventId, COUNT(r) FROM Request r WHERE r.eventId IN :eventIds AND r.status = :status GROUP BY r.eventId")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds, @Param("status") RequestStatus status);

}
