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
                    "You cannot request participation in your own event");
        }


        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException(
                    "Event is not published");
        }


        if (requestRepository.existsByRequesterIdAndEventIdAndStatusNot(
                userId,
                eventId,
                RequestStatus.CANCELED)) {

            throw new ConflictException(
                    "You already have a request for this event");
        }


        Long confirmedCount =
                requestRepository.countConfirmedRequestsByEventId(eventId);


        Integer participantLimit = event.getParticipantLimit();


        if (participantLimit != null
                && participantLimit > 0
                && confirmedCount >= participantLimit) {

            throw new ConflictException(
                    "Participant limit is reached");
        }


        ParticipationRequest request = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .status(RequestStatus.PENDING)
                .build();


        request.setCreated(LocalDateTime.now());


        /*
          Если модерация выключена
          или лимит участников не установлен / равен 0
          заявка сразу подтверждается
         */
        if (!event.getRequestModeration()
                || participantLimit == null
                || participantLimit == 0) {

            request.setStatus(RequestStatus.CONFIRMED);
        }


        request = requestRepository.save(request);

        log.info("Added request with id: {}", request.getId());

        return toDto(request);
    }



    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {

        ParticipationRequest request =
                requestRepository.findByIdAndRequesterId(requestId, userId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Request not found"));


        if (RequestStatus.CONFIRMED.equals(request.getStatus())) {
            throw new ConflictException(
                    "Cannot cancel confirmed request");
        }


        request.setStatus(RequestStatus.CANCELED);

        request = requestRepository.save(request);


        return toDto(request);
    }



    @Override
    public List<ParticipationRequestDto> getEventRequests(
            Long userId,
            Long eventId) {


        Event event = getEventEntity(eventId);


        if (!event.getInitiator().getId().equals(userId)) {

            throw new ConflictException(
                    "You are not the initiator of this event");
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
                    "You are not the initiator of this event");
        }


        if (!EventState.PUBLISHED.equals(event.getState())) {

            throw new ConflictException(
                    "Event must be published");
        }


        Long confirmedCount =
                requestRepository.countConfirmedRequestsByEventId(eventId);


        Integer participantLimit = event.getParticipantLimit();


        List<ParticipationRequest> requests =
                requestRepository.findAllById(
                        updateRequest.getRequestIds());


        for (ParticipationRequest request : requests) {


            if (!RequestStatus.PENDING.equals(request.getStatus())) {

                throw new ConflictException(
                        "Request status must be PENDING");
            }


            if (RequestStatus.CONFIRMED.name()
                    .equals(updateRequest.getStatus())) {


                if (participantLimit != null
                        && confirmedCount >= participantLimit) {

                    throw new ConflictException(
                            "Participant limit reached");
                }


                request.setStatus(RequestStatus.CONFIRMED);

                confirmedCount++;


            } else if (RequestStatus.REJECTED.name()
                    .equals(updateRequest.getStatus())) {


                request.setStatus(RequestStatus.REJECTED);
            }
        }


        requestRepository.saveAll(requests);


        List<ParticipationRequestDto> confirmed =
                requests.stream()
                        .filter(r ->
                                RequestStatus.CONFIRMED.equals(
                                        r.getStatus()))
                        .map(this::toDto)
                        .collect(Collectors.toList());


        List<ParticipationRequestDto> rejected =
                requests.stream()
                        .filter(r ->
                                RequestStatus.REJECTED.equals(
                                        r.getStatus()))
                        .map(this::toDto)
                        .collect(Collectors.toList());


        return new EventRequestStatusUpdateResult(
                confirmed,
                rejected);
    }



    private User getUserEntity(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found"));
    }



    private Event getEventEntity(Long eventId) {

        return eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Event not found"));
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