package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.mapper.HitMapper;
import ru.practicum.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private final HitRepository hitRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto endpointHitDto) {
        var hit = HitMapper.toEntity(endpointHitDto);
        hitRepository.save(hit);
        log.info("Saved hit: {} {} from {}", endpointHitDto.getApp(), endpointHitDto.getUri(), endpointHitDto.getIp());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        List<Object[]> results;
        if (Boolean.TRUE.equals(unique)) {
            results = hitRepository.findUniqueStats(start, end, uris);
        } else {
            results = hitRepository.findStats(start, end, uris);
        }

        return results.stream()
                .map(row -> ViewStatsDto.builder()
                        .app((String) row[0])
                        .uri((String) row[1])
                        .hits((Long) row[2])
                        .build())
                .collect(Collectors.toList());
    }
}