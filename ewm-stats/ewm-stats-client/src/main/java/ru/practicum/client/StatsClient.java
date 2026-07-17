package ru.practicum.client;

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
public class StatsClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;

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

        restTemplate.postForEntity(serverUrl + "/hit", requestEntity, Void.class);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String url = serverUrl + "/stats?start=" + start.format(formatter) + "&end=" + end.format(formatter);
        if (uris != null && !uris.isEmpty()) {
            url += "&uris=" + String.join(",", uris);
        }
        url += "&unique=" + unique;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<ViewStatsDto[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                ViewStatsDto[].class
        );

        return List.of(response.getBody());
    }
}