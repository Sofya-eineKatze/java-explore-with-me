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
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String serverUrl;

    private static final String DATE_TIME_FORMAT =
            "yyyy-MM-dd HH:mm:ss";


    public StatsClient(
            @Value("${stats-server.url:http://localhost:9090}")
            String serverUrl) {

        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
    }


    public void saveHit(
            String app,
            String uri,
            String ip,
            LocalDateTime timestamp) {


        EndpointHitDto hit =
                EndpointHitDto.builder()
                        .app(app)
                        .uri(uri)
                        .ip(ip)
                        .timestamp(timestamp)
                        .build();


        HttpEntity<EndpointHitDto> entity =
                new HttpEntity<>(hit);


        try {

            restTemplate.postForEntity(
                    serverUrl + "/hit",
                    entity,
                    Void.class
            );

        } catch (Exception e) {

            log.error(
                    "Failed to send hit: {}",
                    e.getMessage()
            );
        }
    }



    public List<ViewStatsDto> getStats(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            boolean unique) {


        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);


        StringBuilder url =
                new StringBuilder(serverUrl)
                        .append("/stats?")
                        .append("start=")
                        .append(start.format(formatter))
                        .append("&end=")
                        .append(end.format(formatter));


        if (uris != null && !uris.isEmpty()) {

            url.append("&uris=")
                    .append(String.join(",", uris));
        }


        url.append("&unique=")
                .append(unique);



        try {

            ResponseEntity<ViewStatsDto[]> response =
                    restTemplate.exchange(
                            url.toString(),
                            HttpMethod.GET,
                            null,
                            ViewStatsDto[].class
                    );


            return response.getBody() == null
                    ? List.of()
                    : Arrays.asList(response.getBody());


        } catch (Exception e) {

            log.error(
                    "Failed to get stats: {}",
                    e.getMessage()
            );

            return List.of();
        }
    }
}