package ru.practicum.service.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.Constants;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public List<EventShortDto> getPublishedEvents(String text,
                                                  List<Long> categories,
                                                  Boolean paid,
                                                  LocalDateTime rangeStart,
                                                  LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable,
                                                  String sort,
                                                  int from,
                                                  int size,
                                                  HttpServletRequest request) {

        Pageable pageable = PageRequest.of(from / size, size);

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart must not be after rangeEnd");
        }

        List<Event> events = eventRepository.findPublishedEvents(
                text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                onlyAvailable,
                pageable
        );

        if (request != null) {
            statsClient.saveHit(
                    Constants.APP_NAME,
                    "/events",
                    request.getRemoteAddr(),
                    LocalDateTime.now()
            );
        }

        return events.stream()
                .map(this::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findPublishedEventById(eventId);
        if (event == null) {
            throw new EntityNotFoundException("Event not found or not published");
        }

        if (request != null) {
            statsClient.saveHit(
                    Constants.APP_NAME,
                    "/events/" + eventId,
                    request.getRemoteAddr(),
                    LocalDateTime.now()
            );
        }

        event.setViews(event.getViews() == null ? 1 : event.getViews() + 1);
        eventRepository.save(event);

        return toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        User user = getUserEntity(userId);
        Category category = getCategoryEntity(newEventDto.getCategory());

        if (newEventDto.getEventDate()
                .isBefore(LocalDateTime.now()
                        .plusHours(Constants.MIN_HOURS_BEFORE_EVENT))) {

            throw new IllegalArgumentException(
                    "Event date must be at least "
                            + Constants.MIN_HOURS_BEFORE_EVENT
                            + " hours later");
        }

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .title(newEventDto.getTitle())
                .eventDate(newEventDto.getEventDate())
                .category(category)
                .initiator(user)
                .lat(newEventDto.getLocation().getLat())
                .lon(newEventDto.getLocation().getLon())
                .paid(newEventDto.getPaid() != null
                        ? newEventDto.getPaid()
                        : false)
                .participantLimit(newEventDto.getParticipantLimit() != null
                        ? newEventDto.getParticipantLimit()
                        : 0)
                .requestModeration(newEventDto.getRequestModeration() != null
                        ? newEventDto.getRequestModeration()
                        : true)
                .state(Constants.EVENT_STATE_PENDING)
                .createdOn(LocalDateTime.now())
                .views(0L)
                .build();

        event = eventRepository.save(event);
        log.info("Added event with id: {}", event.getId());

        return toFullDto(event);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        getUserEntity(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        return events.stream()
                .map(this::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        getUserEntity(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId);
        if (event == null) {
            throw new EntityNotFoundException("Event not found or you are not the initiator");
        }
        return toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId,
                                    Long eventId,
                                    UpdateEventRequest updateRequest) {

        getUserEntity(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId);

        if (event == null) {
            throw new EntityNotFoundException(
                    "Event not found or you are not the initiator");
        }

        if (Constants.EVENT_STATE_PUBLISHED.equals(event.getState())) {
            throw new IllegalArgumentException(
                    "Cannot update published event");
        }

        // Обновление полей
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getEventDate() != null) {

            if (updateRequest.getEventDate()
                    .isBefore(LocalDateTime.now()
                            .plusHours(Constants.MIN_HOURS_BEFORE_EVENT))) {

                throw new IllegalArgumentException(
                        "Event date must be at least "
                                + Constants.MIN_HOURS_BEFORE_EVENT
                                + " hours later");
            }

            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getCategory() != null) {
            event.setCategory(
                    getCategoryEntity(updateRequest.getCategory()));
        }

        if (updateRequest.getLocation() != null) {
            event.setLat(updateRequest.getLocation().getLat());
            event.setLon(updateRequest.getLocation().getLon());
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {

            if (updateRequest.getParticipantLimit() < 0) {
                throw new IllegalArgumentException(
                        "Participant limit must not be negative");
            }

            event.setParticipantLimit(
                    updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(
                    updateRequest.getRequestModeration());
        }

        if (updateRequest.getStateAction() != null) {

            switch (updateRequest.getStateAction()) {

                case Constants.STATE_ACTION_SEND_TO_REVIEW:
                    event.setState(Constants.EVENT_STATE_PENDING);
                    break;

                case Constants.STATE_ACTION_CANCEL_REVIEW:
                    event.setState(Constants.EVENT_STATE_CANCELED);
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown state action: "
                                    + updateRequest.getStateAction());
            }
        }

        event = eventRepository.save(event);
        log.info("Updated event with id: {}", event.getId());

        return toFullDto(event);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now().minusYears(100);
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart must not be after rangeEnd");
        }

        List<Event> events = eventRepository.findEventsByAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        return events.stream()
                .map(this::toFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto moderateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Обновление полей (админ может менять всё)
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }

        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(Constants.MIN_HOURS_BEFORE_EVENT))) {
                throw new IllegalArgumentException("Event date must be at least " + Constants.MIN_HOURS_BEFORE_EVENT + " hours later");
            }
            event.setEventDate(updateRequest.getEventDate());
        }

        if (updateRequest.getCategory() != null) {
            event.setCategory(getCategoryEntity(updateRequest.getCategory()));
        }

        if (updateRequest.getLocation() != null) {
            event.setLat(updateRequest.getLocation().getLat());
            event.setLon(updateRequest.getLocation().getLon());
        }

        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }

        if (updateRequest.getParticipantLimit() != null) {
            if (updateRequest.getParticipantLimit() < 0) {
                throw new IllegalArgumentException("Participant limit must not be negative");
            }
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }

        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }

        // Обработка статуса
        if (updateRequest.getStateAction() == null) {
            throw new IllegalArgumentException("State action is required");
        }

        switch (updateRequest.getStateAction()) {
            case Constants.STATE_ACTION_PUBLISH:
                if (!Constants.EVENT_STATE_PENDING.equals(event.getState())) {
                    throw new ConflictException("Event must be in PENDING state to publish");
                }
                event.setState(Constants.EVENT_STATE_PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;

            case Constants.STATE_ACTION_REJECT:
                if (Constants.EVENT_STATE_PUBLISHED.equals(event.getState())) {
                    throw new ConflictException("Cannot reject published event");
                }
                event.setState(Constants.EVENT_STATE_REJECTED);
                break;

            default:
                throw new IllegalArgumentException("Unknown state action: " + updateRequest.getStateAction());
        }

        event = eventRepository.save(event);
        log.info("Moderated event with id: {}, new state: {}", eventId, event.getState());
        return toFullDto(event);
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    private Category getCategoryEntity(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
    }

    private EventShortDto toShortDto(Event event) {
        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(event.getId());

        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryDto.builder()
                        .id(event.getCategory().getId())
                        .name(event.getCategory().getName())
                        .build())
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }

    private EventFullDto toFullDto(Event event) {
        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(event.getId());

        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryDto.builder()
                        .id(event.getCategory().getId())
                        .name(event.getCategory().getName())
                        .build())
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserShortDto.builder()
                        .id(event.getInitiator().getId())
                        .name(event.getInitiator().getName())
                        .build())
                .location(LocationDto.builder()
                        .lat(event.getLat())
                        .lon(event.getLon())
                        .build())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(event.getViews())
                .build();
    }
}