package ru.practicum.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.Constants;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.ParticipationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ParticipationServiceImpl implements ParticipationService {
    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        getUserEntity(userId);
        return requestRepository.findByRequesterId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID must not be null");
        }

        User user = getUserEntity(userId);
        Event event = getEventEntity(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("You cannot request participation in your own event");
        }

        if (!Constants.EVENT_STATE_PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Event is not published");
        }

        if (requestRepository.existsByRequesterIdAndEventIdAndStatusNot(userId, eventId, Constants.REQUEST_STATUS_CANCELED)) {
            throw new ConflictException("You already have a pending request for this event");
        }

        Long confirmedCount = requestRepository.countConfirmedRequestsByEventId(eventId);
        Integer participantLimit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0;

        if (participantLimit > 0 && confirmedCount >= participantLimit) {
            throw new ConflictException("Participant limit is reached");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .created(LocalDateTime.now().withNano((LocalDateTime.now().getNano() / 1000) * 1000))
                .status(Constants.REQUEST_STATUS_PENDING)
                .build();

        if (!event.getRequestModeration()) {
            request.setStatus(Constants.REQUEST_STATUS_CONFIRMED);
        }

        if (participantLimit == 0) {
            request.setStatus(Constants.REQUEST_STATUS_CONFIRMED);
        }

        request = requestRepository.save(request);
        log.info("Added request with id: {}", request.getId());
        return toDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Request not found"));

        request.setStatus(Constants.REQUEST_STATUS_CANCELED);
        request = requestRepository.save(request);
        log.info("Cancelled request with id: {}", requestId);
        return toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = getEventEntity(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("You are not the initiator of this event");
        }

        return requestRepository.findByEventId(eventId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = getEventEntity(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("You are not the initiator of this event");
        }

        if (!Constants.EVENT_STATE_PENDING.equals(event.getState())) {
            throw new IllegalArgumentException("Event is not in pending state");
        }

        Long confirmedCount = requestRepository.countConfirmedRequestsByEventId(eventId);
        Integer participantLimit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0;

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        for (ParticipationRequest request : requests) {
            if (!Constants.REQUEST_STATUS_PENDING.equals(request.getStatus())) {
                throw new IllegalArgumentException("Request status must be PENDING");
            }

            if (Constants.REQUEST_STATUS_CONFIRMED.equals(updateRequest.getStatus())) {
                if (participantLimit == 0 || confirmedCount < participantLimit) {
                    request.setStatus(Constants.REQUEST_STATUS_CONFIRMED);
                    confirmedCount++;
                } else {
                    request.setStatus(Constants.REQUEST_STATUS_REJECTED);
                }
            } else if (Constants.REQUEST_STATUS_REJECTED.equals(updateRequest.getStatus())) {
                request.setStatus(Constants.REQUEST_STATUS_REJECTED);
            }
        }

        requestRepository.saveAll(requests);

        List<ParticipationRequestDto> confirmed = requests.stream()
                .filter(r -> Constants.REQUEST_STATUS_CONFIRMED.equals(r.getStatus()))
                .map(this::toDto)
                .collect(Collectors.toList());

        List<ParticipationRequestDto> rejected = requests.stream()
                .filter(r -> Constants.REQUEST_STATUS_REJECTED.equals(r.getStatus()))
                .map(this::toDto)
                .collect(Collectors.toList());

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    private Event getEventEntity(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));
    }

    private ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .build();
    }
}