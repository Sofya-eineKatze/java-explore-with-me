package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Hit;
import ru.practicum.projection.ViewStatsProjection;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {

    @Query("""
            SELECT h.app AS app,
                   h.uri AS uri,
                   COUNT(h.id) AS hits
            FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR h.uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.id) DESC
            """)
    List<ViewStatsProjection> findStats(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("uris") List<String> uris);

    @Query("""
            SELECT h.app AS app,
                   h.uri AS uri,
                   COUNT(DISTINCT h.ip) AS hits
            FROM Hit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR h.uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC
            """)
    List<ViewStatsProjection> findUniqueStats(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("uris") List<String> uris);
}