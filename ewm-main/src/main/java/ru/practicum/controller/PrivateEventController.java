package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.UpdateEventRequest;
import ru.practicum.service.EventService;
import jakarta.validation.constraints.Min;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventController {

    private final EventService eventService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto newEventDto) {

        log.info("POST /users/{}/events - add event by user", userId);

        return eventService.addEvent(userId, newEventDto);
    }


    @GetMapping
    public List<EventShortDto> getUserEvents(
            @PathVariable Long userId,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int from,

            @RequestParam(defaultValue = "10")
            @Min(1)
            int size) {

        log.info("GET /users/{}/events - get user events", userId);

        return eventService.getUserEvents(userId, from, size);
    }


    @GetMapping("/{eventId}")
    public EventFullDto getUserEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("GET /users/{}/events/{} - get user event",
                userId, eventId);

        return eventService.getUserEventById(userId, eventId);
    }


    @PatchMapping("/{eventId}")
    public EventFullDto updateUserEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest updateRequest) {

        log.info("PATCH /users/{}/events/{} - update user event",
                userId, eventId);

        return eventService.updateEvent(userId, eventId, updateRequest);
    }
}