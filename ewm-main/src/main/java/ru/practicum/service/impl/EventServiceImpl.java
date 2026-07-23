package ru.practicum.service.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.Constants;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final EventMapper eventMapper;

    @Override
    public List<EventShortDto> getPublishedEvents(EventSearchParams params, HttpServletRequest request) {

        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart must not be after rangeEnd");
        }

        Sort sortBy = Sort.unsorted();
        if (Constants.SORT_EVENT_DATE.equalsIgnoreCase(params.getSort())) {
            sortBy = Sort.by("eventDate").ascending();
        }

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(), sortBy);

        List<Event> events = eventRepository.findPublishedEvents(
                params.getText(),
                params.getCategories(),
                params.getPaid(),
                rangeStart,
                rangeEnd,
                params.getOnlyAvailable(),
                pageable
        );

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);

        if (request != null) {
            statsClient.saveHit(
                    Constants.APP_NAME,
                    "/events",
                    request.getRemoteAddr(),
                    LocalDateTime.now()
            );
        }

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublishedEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findPublishedEventById(eventId);
        if (event == null) {
            throw new EntityNotFoundException("Event not found or not published");
        }

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        Map<Long, Long> views = getViews(List.of(event));

        if (request != null) {
            statsClient.saveHit(
                    Constants.APP_NAME,
                    "/events/" + eventId,
                    request.getRemoteAddr(),
                    LocalDateTime.now()
            );
        }

        return eventMapper.toFullDto(event, confirmedRequests, views.getOrDefault(eventId, 0L));
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
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();

        event = eventRepository.save(event);
        log.info("Added event with id: {}", event.getId());

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(event.getId());
        Map<Long, Long> views = getViews(List.of(event));

        return eventMapper.toFullDto(event, confirmedRequests, views.getOrDefault(event.getId(), 0L));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        getUserEntity(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);

        return events.stream()
                .map(event -> eventMapper.toShortDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        getUserEntity(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId);
        if (event == null) {
            throw new EntityNotFoundException("Event not found or you are not the initiator");
        }

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        Map<Long, Long> views = getViews(List.of(event));

        return eventMapper.toFullDto(event, confirmedRequests, views.getOrDefault(eventId, 0L));
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

        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException(
                    "Cannot update published event");
        }

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
                    event.setState(EventState.PENDING);
                    break;

                case Constants.STATE_ACTION_CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown state action: "
                                    + updateRequest.getStateAction());
            }
        }

        event = eventRepository.save(event);
        log.info("Updated event with id: {}", event.getId());

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        Map<Long, Long> views = getViews(List.of(event));

        return eventMapper.toFullDto(event, confirmedRequests, views.getOrDefault(eventId, 0L));
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params) {

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now().minusYears(100);
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart must not be after rangeEnd");
        }

        List<Event> events = eventRepository.findEventsByAdmin(
                params.getUsers(),
                params.getStates(),
                params.getCategories(),
                rangeStart,
                rangeEnd,
                pageable
        );

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);

        return events.stream()
                .map(event -> eventMapper.toFullDto(
                        event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto moderateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

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
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new IllegalArgumentException("Event date must be at least 1 hour later");
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

        if (updateRequest.getStateAction() != null) {

            switch (updateRequest.getStateAction()) {

                case Constants.STATE_ACTION_PUBLISH:
                    if (!EventState.PENDING.equals(event.getState())) {
                        throw new ConflictException("Event must be in PENDING state to publish");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case Constants.STATE_ACTION_REJECT:
                    if (EventState.PUBLISHED.equals(event.getState())) {
                        throw new ConflictException("Cannot reject published event");
                    }
                    event.setState(EventState.REJECTED);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown state action: " + updateRequest.getStateAction());
            }
        }

        event = eventRepository.save(event);
        log.info("Moderated event with id: {}, new state: {}", eventId, event.getState());

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        Map<Long, Long> views = getViews(List.of(event));

        return eventMapper.toFullDto(event, confirmedRequests, views.getOrDefault(eventId, 0L));
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {

        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        return requestRepository
                .countConfirmedRequestsByEventIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        LocalDateTime end = LocalDateTime.now();

        try {
            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.getUri().replace("/events/", "")),
                            ViewStatsDto::getHits
                    ));
        } catch (Exception e) {
            log.warn("Failed to get views from stats server: {}", e.getMessage());
            return Map.of();
        }
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    private Category getCategoryEntity(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
    }
}