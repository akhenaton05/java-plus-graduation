package ru.practicum.users.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event_service.dto.EventFullDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.dto.NewEventDto;
import ru.practicum.event_service.dto.UpdateEventUserRequest;
import ru.practicum.events.model.Location;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.EventRequestStatusUpdateResult;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.request_service.feign.RequestClient;
import ru.practicum.user_service.config.DateConfig;
import ru.practicum.user_service.errors.ForbiddenActionException;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.user_service.dto.GetUserEventsDto;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.feign.UserClient;
import ru.practicum.request_service.entity.ParticipationRequestStatus;
import ru.practicum.request_service.entity.RequestUpdateStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PrivateUserEventServiceImpl implements PrivateUserEventService {
    private EventRepository eventRepository;
    private UserClient userClient;
    private CategoryRepository categoryRepository;
    private RequestClient requestClient;
    private EventMapper eventMapper;

    @Override
    public List<EventShortDto> getUsersEvents(GetUserEventsDto dto) {
        UserShortDto user =  userClient.getUser(dto.getUserId()).getBody();
        PageRequest page = PageRequest.of(dto.getFrom() > 0 ? dto.getFrom() / dto.getSize() : 0, dto.getSize());
        return eventRepository.findAllByInitiatorId(user.getId(), page).stream()
                .map(eventMapper::toEventShortDto)
                .toList();
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));
        return eventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto addNewEvent(Long userId, NewEventDto eventDto) {
        log.info("IN ADD NEW EVENT");
        UserShortDto user =  userClient.getUser(userId).getBody();
        log.info("GOT user = {}", user);
        Event event = eventMapper.dtoToEvent(eventDto, user.getId());
        log.info("EVENT MAPPED{}", event);
        eventRepository.save(event);

        return eventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateDto) {
        UserShortDto user =  userClient.getUser(userId).getBody();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ForbiddenActionException("User is not the event creator");
        }

        if (Objects.equals(event.getState(), StateEvent.PUBLISHED)) {
            throw new ForbiddenActionException("Changing of published event is forbidden.");
        }
        Optional.ofNullable(updateDto.getTitle()).ifPresent(event::setTitle);
        Optional.ofNullable(updateDto.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(updateDto.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(updateDto.getEventDate()).map(this::parseEventDate).ifPresent(event::setEventDate);
        Optional.ofNullable(updateDto.getLocation())
                .ifPresent(dto -> event.setLocation(Location.builder()
                        .lat(dto.getLat())
                        .lon(dto.getLon())
                        .build()));

        if (updateDto.getCategory() != 0) {
            event.setCategory(categoryRepository.findById((long) updateDto.getCategory())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + updateDto.getCategory())));
        }

        updateEventState(event, updateDto.getStateAction());

        event.setRequestModeration(updateDto.isRequestModeration());
        event.setInitiatorId(user.getId());

        return eventMapper.toEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<ParticipationRequestDto> getUserEventRequests(Long userId, Long eventId) {
        eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId + " for user " + userId));
        return requestClient.findRequestsByEventId(eventId).getBody();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateUserEventRequest(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        UserShortDto user =  userClient.getUser(userId).getBody();
        Event event = getEventWithConfirmedRequests(eventId);
        List<ParticipationRequestDto> participation = requestClient.findByRequestIds(request.getRequestIds()).getBody();
        for (ParticipationRequestDto req : participation) {
            if (!req.getStatus().equals(ParticipationRequestStatus.PENDING)) {
                throw new ForbiddenActionException("request status should be PENDING");
            }
        }

        int partLimit = event.getParticipantLimit();
        int confPart = Objects.nonNull(event.getConfirmedRequests()) ? event.getConfirmedRequests() : 0;
        int diff = partLimit - confPart;

        if (diff >= request.getRequestIds().size()) {
            requestClient.updateStatusByIds(request);
            if (RequestUpdateStatus.valueOf(request.getStatus()).equals(RequestUpdateStatus.CONFIRMED)) {

                for (ParticipationRequestDto req : participation) {
                    req.setStatus(ParticipationRequestStatus.CONFIRMED);
                }

                return EventRequestStatusUpdateResult.builder()
                        .confirmedRequests(participation)
                        .build();
            } else {
                for (ParticipationRequestDto req : participation) {
                    req.setStatus(ParticipationRequestStatus.REJECTED);
                }

                return EventRequestStatusUpdateResult.builder()
                        .rejectedRequests(participation)
                        .build();
            }
        } else if (diff == 0) {
            throw new ForbiddenActionException("Participation limit is 0");
        } else {
            List<Long> confirmed = new ArrayList<>();
            List<Long> rejected = new ArrayList<>();
            for (int i = 1; i <= request.getRequestIds().size(); i++) {
                if (i > diff) {
                    rejected.add(request.getRequestIds().get(i));
                } else confirmed.add(request.getRequestIds().get(i));
            }

            requestClient.updateStatusByIds(new EventRequestStatusUpdateRequest(confirmed, ParticipationRequestStatus.CONFIRMED.toString()));
            requestClient.updateStatusByIds(new EventRequestStatusUpdateRequest(rejected, ParticipationRequestStatus.REJECTED.toString()));

            EventRequestStatusUpdateResult res = new EventRequestStatusUpdateResult();

            for (ParticipationRequestDto req : participation) {
                for (Long id : confirmed) {
                    if (Objects.equals(req.getId(), id)) {
                        req.setStatus(ParticipationRequestStatus.CONFIRMED);
                    }
                }
                req.setStatus(ParticipationRequestStatus.CANCELED);
            }

            List<ParticipationRequestDto> updatedRequestsConfirmed = participation.stream()
                    .filter(req -> confirmed.contains(req.getId()))
                    .peek(req -> req.setStatus(ParticipationRequestStatus.CONFIRMED))
                    .toList();

            List<ParticipationRequestDto> updatedRequestsRejected = participation.stream()
                    .filter(req -> rejected.contains(req.getId()))
                    .peek(req -> req.setStatus(ParticipationRequestStatus.REJECTED))
                    .toList();

            res.setConfirmedRequests(updatedRequestsConfirmed);
            res.setRejectedRequests(updatedRequestsRejected);

            return res;
        }
    }

    private LocalDateTime parseEventDate(String date) {
        return LocalDateTime.parse(date, DateConfig.FORMATTER);
    }

    private void updateEventState(Event event, String stateAction) {
        if (Objects.isNull(stateAction)) return;

        if ("PUBLISH_REVIEW".equals(stateAction)) {
            throw new ForbiddenActionException("Publishing this event is forbidden.");
        }

        switch (stateAction) {
            case "CANCEL_REVIEW":
                event.setState(StateEvent.CANCELED);
                break;
            case "SEND_TO_REVIEW":
                event.setState(StateEvent.PENDING);
                break;
            default:
                throw new IllegalArgumentException("Invalid state action: " + stateAction);
        }
    }

    public Event getEventWithConfirmedRequests(Long eventId) {
        return eventRepository.findEventWithStatus(eventId, ParticipationRequestStatus.CONFIRMED);
    }
}
