package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.statsdto.HitDto;
import ru.practicum.statsdto.StatsDtoOut;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
public abstract class StatsClient {

    private final RestClient restClient;
    private final String serverUrl;

    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";

    public StatsClient(String serverUrl) {
        this.serverUrl = serverUrl;

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));

        restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(serverUrl)
                .build();
    }

    public void hit(String service, String url, String ip) {
        HitDto dto = new HitDto();
        dto.setService(service);
        dto.setUri(url);
        dto.setIp(ip);
        dto.setDateTime(LocalDateTime.now());
        this.hit(dto);
    }

    public void hit(HitDto hitDto) throws StatsClientException {

        try {
            restClient.post()
                    .uri(HIT_ENDPOINT)
                    .contentType(APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();

        } catch (ResourceAccessException e) {
            log.error(e.getMessage());
            throw new StatsClientException("Connection to stats service failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in hit(): {}", e.getMessage());
            throw new StatsClientException("Failed to save hit: " + e.getMessage());
        }
    }

    public Collection<StatsDtoOut> getStats(LocalDateTime start,
                                            LocalDateTime end,
                                            Collection<String> uris,
                                            boolean unique) throws StatsClientException {
        validateDates(start, end);

        String url = UriComponentsBuilder.fromHttpUrl(serverUrl + STATS_ENDPOINT)
                .queryParam("start", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .queryParam("end", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .toUriString();

        log.debug("Requesting stats from: {}", url);

        try {
            StatsDtoOut[] stats = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("Stats service error: " + res.getStatusCode());
                    })
                    .body(StatsDtoOut[].class);
            return stats != null ? Arrays.asList(stats) : Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.error("Stats service unavailable. URL: {}, error: {}", url, e.getMessage());
            throw new StatsClientException("Connection to stats service failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in getStats(): {}", e.getMessage());
            throw new StatsClientException("Failed to get stats: " + e.getMessage());
        }
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Dates must not be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }
}
