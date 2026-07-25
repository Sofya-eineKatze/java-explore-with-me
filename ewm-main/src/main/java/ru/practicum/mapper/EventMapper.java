package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.*;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;

@Component
public class EventMapper {


    public EventShortDto toShortDto(
            Event event,
            Long confirmedRequests,
            Long views) {

        if (event == null) {
            return null;
        }

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
                .views(views)
                .build();
    }


    public EventFullDto toFullDto(
            Event event,
            Long confirmedRequests,
            Long views) {

        if (event == null) {
            return null;
        }

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
                .state(event.getState() != null
                        ? event.getState().name()
                        : null)
                .title(event.getTitle())
                .views(views)
                .build();
    }


    public Event toEntity(
            NewEventDto dto,
            User user,
            Category category) {

        if (dto == null) {
            return null;
        }


        return Event.builder()
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .title(dto.getTitle())
                .eventDate(dto.getEventDate())
                .category(category)
                .initiator(user)
                .lat(dto.getLocation().getLat())
                .lon(dto.getLocation().getLon())
                .paid(dto.getPaid() != null
                        ? dto.getPaid()
                        : false)
                .participantLimit(dto.getParticipantLimit() != null
                        ? dto.getParticipantLimit()
                        : 0)
                .requestModeration(dto.getRequestModeration() != null
                        ? dto.getRequestModeration()
                        : true)
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
    }
}