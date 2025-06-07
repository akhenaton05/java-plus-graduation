package ru.practicum.request_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/users/{userId}/requests")
    ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId);

    @PostMapping("/users/{userId}/requests")
    ResponseEntity<ParticipationRequestDto> addParticipationRequest(@PathVariable Long userId, @RequestParam Long eventId);

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    ResponseEntity<ParticipationRequestDto> cancelRequest(@PathVariable Long userId, @PathVariable Long requestId);

    @GetMapping("/requests/by-event")
    ResponseEntity<List<ParticipationRequestDto>> findRequestsByEventId(@RequestParam Long eventId);

    @GetMapping("/requests/by-request-ids")
    ResponseEntity<List<ParticipationRequestDto>> findByRequestIds(@RequestParam List<Long> requestIds);

    @PostMapping("/requests/update-request-ids")
    void updateStatusByIds(@RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("/requests/exists")
    boolean existsByUserIdAndEventId(@RequestParam("checkUserId") Long checkUserId, @RequestParam Long eventId);

    @GetMapping("/requests/confirmed-count")
    ResponseEntity<Integer> getConfirmedRequestsCount(@RequestParam Long eventId);

    @GetMapping("/requests/confirmed-counts")
    ResponseEntity<Map<Long, Integer>> getConfirmedRequestsCounts(@RequestParam List<Long> eventIds);
}