package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.*;

import java.util.List;

public interface EventService {

    // Публичные методы
    List<EventShortDto> getPublishedEvents(EventSearchParams params, HttpServletRequest request);

    EventFullDto getPublishedEventById(Long eventId, HttpServletRequest request);

    // Приватные методы
    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventRequest updateRequest);

    // Административные методы
    List<EventFullDto> getEventsByAdmin(AdminEventSearchParams params);

    EventFullDto moderateEvent(Long eventId, UpdateEventAdminRequest updateRequest);
}