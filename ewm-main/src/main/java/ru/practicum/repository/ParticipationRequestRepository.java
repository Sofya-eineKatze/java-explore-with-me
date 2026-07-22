package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long userId);

    List<ParticipationRequest> findByEventId(Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);


    @Query("""
            SELECT COUNT(r)
            FROM ParticipationRequest r
            WHERE r.event.id = :eventId
              AND r.status = 'CONFIRMED'
            """)
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);


    @Query("""
            SELECT r.event.id, COUNT(r)
            FROM ParticipationRequest r
            WHERE r.status = 'CONFIRMED'
              AND r.event.id IN :eventIds
            GROUP BY r.event.id
            """)
    List<Object[]> countConfirmedRequestsByEventIds(
            @Param("eventIds") List<Long> eventIds);


    boolean existsByRequesterIdAndEventIdAndStatusNot(
            Long userId,
            Long eventId,
            String status);
}