package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class StatsClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public StatsClient(@Value("${stats-server.url:http://localhost:9090}") String serverUrl) {
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
    }

    public void saveHit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(hit, headers);

        try {
            restTemplate.postForEntity(serverUrl + "/hit", requestEntity, Void.class);
            log.info("Hit sent to stats server: {} {} from {}", app, uri, ip);
        } catch (Exception e) {
            log.error("Failed to send hit to stats server: {}", e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

        StringBuilder url = new StringBuilder(serverUrl + "/stats?");
        url.append("start=").append(start.format(formatter));
        url.append("&end=").append(end.format(formatter));

        if (uris != null && !uris.isEmpty()) {
            url.append("&uris=").append(String.join(",", uris));
        }
        url.append("&unique=").append(unique);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ViewStatsDto[]> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    requestEntity,
                    ViewStatsDto[].class
            );
            return List.of(response.getBody());
        } catch (Exception e) {
            log.error("Failed to get stats from stats server: {}", e.getMessage());
            return List.of();
        }
    }
}