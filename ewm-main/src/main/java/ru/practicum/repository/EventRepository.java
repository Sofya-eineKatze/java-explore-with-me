package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            SELECT e
            FROM Event e
            WHERE e.state = 'PUBLISHED'
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND (:paid IS NULL OR e.paid = :paid)
              AND e.eventDate BETWEEN :rangeStart AND :rangeEnd
              AND (
                    :onlyAvailable = false
                    OR e.participantLimit = 0
                    OR e.participantLimit > (
                        SELECT COUNT(r)
                        FROM ParticipationRequest r
                        WHERE r.event.id = e.id
                          AND r.status = 'CONFIRMED'
                    )
              )
            """)
    List<Event> findPublishedEvents(@Param("categories") List<Long> categories,
                                    @Param("paid") Boolean paid,
                                    @Param("rangeStart") LocalDateTime rangeStart,
                                    @Param("rangeEnd") LocalDateTime rangeEnd,
                                    @Param("onlyAvailable") Boolean onlyAvailable,
                                    Pageable pageable);

    @Query("""
            SELECT e
            FROM Event e
            WHERE e.state = 'PUBLISHED'
              AND e.id = :eventId
            """)
    Event findPublishedEventById(@Param("eventId") Long eventId);

    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    Event findByIdAndInitiatorId(Long eventId, Long userId);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:users IS NULL OR e.initiator.id IN :users)
              AND (:states IS NULL OR e.state IN :states)
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND e.eventDate BETWEEN :rangeStart AND :rangeEnd
            """)
    List<Event> findEventsByAdmin(@Param("users") List<Long> users,
                                  @Param("states") List<String> states,
                                  @Param("categories") List<Long> categories,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  Pageable pageable);
}