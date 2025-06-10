package ru.practicum.request_service.feign;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.user_service.exceptions.ServiceUnavailableException;

import java.util.List;
import java.util.Map;

@Component
public class RequestClientFallback {
    @GetMapping("/requests/by-event")
    ResponseEntity<List<ParticipationRequestDto>> findRequestsByEventId(@RequestParam Long eventId) {
        throw new ServiceUnavailableException("RequestService unavailable");
    }

    @GetMapping("/requests/by-request-ids")
    ResponseEntity<List<ParticipationRequestDto>> findByRequestIds(@RequestParam List<Long> requestIds){
        throw new ServiceUnavailableException("RequestService unavailable");
    }

    @PostMapping("/requests/update-request-ids")
    void updateStatusByIds(@RequestBody EventRequestStatusUpdateRequest request) {
        throw new ServiceUnavailableException("RequestService unavailable");
    }

    @GetMapping("/requests/exists")
    boolean existsByUserIdAndEventId(@RequestParam("checkUserId") Long checkUserId, @RequestParam Long eventId) {
        throw new ServiceUnavailableException("RequestService unavailable");
    }

    @GetMapping("/requests/confirmed-count")
    ResponseEntity<Integer> getConfirmedRequestsCount(@RequestParam Long eventId) {
        throw new ServiceUnavailableException("RequestService unavailable");
    }

    @GetMapping("/requests/confirmed-counts")
    ResponseEntity<Map<Long, Integer>> getConfirmedRequestsCounts(@RequestParam List<Long> eventIds) {
        throw new ServiceUnavailableException("RequestService unavailable");
    }
}
