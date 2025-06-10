package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PrivateRequestsForParticipation {

    private final ParticipationRequestService participationRequestServiceImpl;

    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId) {
        log.info("Request to get participation requests for user {}", userId);
        List<ParticipationRequestDto> participationRequests = participationRequestServiceImpl.getUserRequests(userId);
        return ResponseEntity.ok(participationRequests);
    }

    @PostMapping("/users/{userId}/requests")
    public ResponseEntity<ParticipationRequestDto> addParticipationRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {
        log.info("Request to add participation request for user {} for event {}", userId, eventId);
        ParticipationRequestDto participationRequest = participationRequestServiceImpl.addParticipationRequest(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(participationRequest);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        log.info("Request to cancel participation request for user {} and request {}", userId, requestId);
        ParticipationRequestDto cancelledRequest = participationRequestServiceImpl.cancelRequest(userId, requestId);
        return ResponseEntity.ok(cancelledRequest);
    }

    @GetMapping("/requests/by-event")
    public ResponseEntity<List<ParticipationRequestDto>> findRequestsByEventId(@RequestParam Long eventId) {
        log.info("Request to get participation requests for event {}", eventId);
        List<ParticipationRequestDto> participationRequests = participationRequestServiceImpl.findRequestsByEventId(eventId);
        return ResponseEntity.ok(participationRequests);
    }

    @GetMapping("/requests/by-request-ids")
    public ResponseEntity<List<ParticipationRequestDto>> findByRequestIds(@RequestParam List<Long> requestIds) {
        log.info("Request to get participation requests for requestIds {}", requestIds);
        List<ParticipationRequestDto> participationRequests = participationRequestServiceImpl.findByRequestIds(requestIds);
        return ResponseEntity.ok(participationRequests);
    }

    @PostMapping("/requests/update-request-ids")
    public void updateStatusByIds(@RequestBody EventRequestStatusUpdateRequest request) {
        log.info("Request to update participation requests for requestIds {}", request);
        participationRequestServiceImpl.updateStatusByIds(request);
    }

    @GetMapping("/requests/exists")
    public boolean existsByUserIdAndEventId(
            @RequestParam("checkUserId") Long checkUserId,
            @RequestParam Long eventId) {
        log.info("Exists check for userId {}, eventId {}", checkUserId, eventId);
        return participationRequestServiceImpl.existsByUserIdAndEventId(checkUserId, eventId);
    }

    @GetMapping("/requests/confirmed-count")
    public ResponseEntity<Integer> getConfirmedRequestsCount(@RequestParam Long eventId) {
        log.info("Request for getting confirmed requests count for event {}", eventId);
        return ResponseEntity.ok(participationRequestServiceImpl.getConfirmedRequestsCount(eventId));
    }

    @GetMapping("/requests/confirmed-counts")
    public ResponseEntity<Map<Long, Integer>> getConfirmedRequestsCounts(@RequestParam List<Long> eventIds) {
        log.info("Request to get confirmed requests counts for events {}", eventIds);
        Map<Long, Integer> counts = eventIds.stream()
                .collect(Collectors.toMap(
                        eventId -> eventId,
                        participationRequestServiceImpl::getConfirmedRequests
                ));
        return ResponseEntity.ok(counts);
    }
}