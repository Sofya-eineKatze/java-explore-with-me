package ru.practicum.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.RequestStatus;
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

        User user = getUserEntity(userId);
        Event event = getEventEntity(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(
                    "You cannot request participation in your own event"
            );
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException(
                    "Event is not published"
            );
        }

        if (requestRepository.existsByRequesterIdAndEventIdAndStatusNot(
                userId,
                eventId,
                RequestStatus.CANCELED.name())) {

            throw new ConflictException(
                    "You already have a request for this event"
            );
        }


        Long confirmedCount =
                requestRepository.countConfirmedRequestsByEventId(eventId);

        Integer limit = event.getParticipantLimit() == null
                ? 0
                : event.getParticipantLimit();


        if (limit > 0 && confirmedCount >= limit) {
            throw new ConflictException(
                    "Participant limit is reached"
            );
        }


        RequestStatus status =
                event.getRequestModeration()
                        ? RequestStatus.PENDING
                        : RequestStatus.CONFIRMED;


        ParticipationRequest request = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .created(LocalDateTime.now())
                .status(status)
                .build();


        return toDto(requestRepository.save(request));
    }


    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {

        ParticipationRequest request =
                requestRepository.findByIdAndRequesterId(requestId, userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Request not found"
                                ));

        request.setStatus(RequestStatus.CANCELED);

        return toDto(requestRepository.save(request));
    }


    @Override
    public List<ParticipationRequestDto> getEventRequests(
            Long userId,
            Long eventId) {

        Event event = getEventEntity(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(
                    "You are not the initiator of this event"
            );
        }


        return requestRepository.findByEventId(eventId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest updateRequest) {


        Event event = getEventEntity(eventId);


        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(
                    "You are not the initiator of this event"
            );
        }


        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException(
                    "Event must be published"
            );
        }


        RequestStatus newStatus =
                RequestStatus.valueOf(updateRequest.getStatus());


        List<ParticipationRequest> requests =
                requestRepository.findAllById(
                        updateRequest.getRequestIds()
                );


        long confirmed =
                requestRepository.countConfirmedRequestsByEventId(eventId);


        int limit =
                event.getParticipantLimit() == null
                        ? 0
                        : event.getParticipantLimit();


        for (ParticipationRequest request : requests) {

            if (!RequestStatus.PENDING.equals(request.getStatus())) {
                throw new ConflictException(
                        "Request status must be PENDING"
                );
            }


            if (RequestStatus.CONFIRMED.equals(newStatus)) {

                if (limit > 0 && confirmed >= limit) {
                    request.setStatus(RequestStatus.REJECTED);
                } else {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed++;
                }

            } else if (RequestStatus.REJECTED.equals(newStatus)) {

                request.setStatus(RequestStatus.REJECTED);
            }
        }


        requestRepository.saveAll(requests);


        return new EventRequestStatusUpdateResult(

                requests.stream()
                        .filter(r ->
                                RequestStatus.CONFIRMED.equals(r.getStatus()))
                        .map(this::toDto)
                        .collect(Collectors.toList()),

                requests.stream()
                        .filter(r ->
                                RequestStatus.REJECTED.equals(r.getStatus()))
                        .map(this::toDto)
                        .collect(Collectors.toList())
        );
    }


    private User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found"
                        ));
    }


    private Event getEventEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Event not found"
                        ));
    }


    private ParticipationRequestDto toDto(
            ParticipationRequest request) {

        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }
}