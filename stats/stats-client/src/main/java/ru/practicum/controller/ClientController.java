package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.CreateEndpointHitDto;
import ru.practicum.dto.ManyEndPointDto;
import ru.practicum.dto.ReadEndpointHitDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public ClientController(DiscoveryClient discoveryClient, String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.retryTemplate = createRetryTemplate();
        this.statsServiceId = statsServiceId;
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    public ResponseEntity<Void> saveView(String addr, String uri) {
        log.info("ClientController.saveView addr {}, uri {}", addr, uri);
        CreateEndpointHitDto dto = new CreateEndpointHitDto(
                "ewm-main-service",
                uri,
                addr,
                LocalDateTime.now()
        );

        restClient.post()
                .uri(makeUri("/hit", Map.of()))
                .body(dto)
                .retrieve()
                .toBodilessEntity();

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public List<ReadEndpointHitDto> getHits(String start, String end, List<String> uris, boolean unique) {
        log.info("ClientController.getHits start {}, end {}, uris {}, unique {}", start, end, uris, unique);

        Map<String, String> params = new HashMap<>();
        params.put("start", start);
        params.put("end", end);
        params.put("uris", String.join(",", uris));
        params.put("unique", String.valueOf(unique));

        ResponseEntity<Collection<ReadEndpointHitDto>> response = restClient.get()
                .uri(makeUri("/stats", params))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });

        return Optional.ofNullable(response.getBody())
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    public ResponseEntity<Void> saveHitsGroup(List<String> uris, String ip) {
        log.info("ClientController.saveHitsGroup uris {}, addr {}", uris, ip);

        ManyEndPointDto manyEndPointDto = ManyEndPointDto.builder()
                .uris(uris)
                .ip(ip)
                .build();

        log.info("ClientController.saveHitsGroup many {}", manyEndPointDto);

        restClient.post()
                .uri(makeUri("/hit/group", Map.of()))
                .body(manyEndPointDto)
                .retrieve()
                .toBodilessEntity();
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private URI makeUri(String path, Map<String, String> queryParams) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(instance.getHost())
                .port(instance.getPort())
                .path(path);

        queryParams.forEach(uriBuilder::queryParam);

        return uriBuilder.build().toUri();
    }

    public static class StatsServerUnavailable extends RuntimeException {
        public StatsServerUnavailable(String message) {
            super(message);
        }

        public StatsServerUnavailable(String message, Throwable cause) {
            super(message, cause);
        }
    }
}



