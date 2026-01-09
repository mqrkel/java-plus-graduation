package ru.practicum.statsclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.statsdto.HitDto;
import ru.practicum.statsdto.StatsDtoOut;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
public abstract class StatsClient {

    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";

    private final String statsServiceId;
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    public StatsClient(String statsServiceId, DiscoveryClient discoveryClient) {
        this.statsServiceId = statsServiceId;
        this.discoveryClient = discoveryClient;

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        this.retryTemplate = buildRetryTemplate();
    }

    private RetryTemplate buildRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);      // 3 секунды между попытками
        template.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);              // 3 попытки
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            log.info("StatsClient: discovered {} instances for id '{}': {}",
                    instances.size(), statsServiceId, instances);

            return instances.stream()
                    .findFirst()
                    .orElseThrow(() -> new StatsClientException("Instance not found: " + statsServiceId));
        } catch (Exception e) {
            log.error("StatsClient: error discovering service '{}': {}", statsServiceId, e.getMessage(), e);
            throw new StatsClientException(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId);
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        URI uri = URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
        log.info("StatsClient: resolved URI {}", uri);
        return uri;
    }

    public void hit(String service, String url, String ip) {
        HitDto dto = new HitDto();
        dto.setService(service);
        dto.setUri(url);
        dto.setIp(ip);
        dto.setDateTime(LocalDateTime.now());
        this.hit(dto);
    }

    public void hit(HitDto hitDto) {
        URI uri = makeUri(HIT_ENDPOINT);

        try {
            restClient.post()
                    .uri(uri)
                    .contentType(APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            log.error("Stats service unavailable. URI: {}, error: {}", uri, e.getMessage());
            throw new StatsClientException("Connection to stats service failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in hit(): {}", e.getMessage());
            throw new StatsClientException("Failed to save hit: " + e.getMessage());
        }
    }

    public Collection<StatsDtoOut> getStats(LocalDateTime start,
                                            LocalDateTime end,
                                            Collection<String> uris,
                                            boolean unique) {

        validateDates(start, end);

        URI base = makeUri(STATS_ENDPOINT);
        String url = UriComponentsBuilder.fromUri(base)
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