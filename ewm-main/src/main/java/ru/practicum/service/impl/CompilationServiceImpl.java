package ru.practicum.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.CompilationService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    //  Публичные методы

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }
        return compilations.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = getCompilationEntity(compId);
        return toDto(compilation);
    }

    // Административные методы

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        List<Event> events = newCompilationDto.getEvents() != null
                ? eventRepository.findAllById(newCompilationDto.getEvents())
                : List.of();

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .events(events)
                .build();

        compilation = compilationRepository.save(compilation);
        log.info("Added compilation with id: {}", compilation.getId());
        return toDto(compilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationEntity(compId);
        compilationRepository.delete(compilation);
        log.info("Deleted compilation with id: {}", compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = getCompilationEntity(compId);

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }
        if (updateRequest.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(updateRequest.getEvents());
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);
        log.info("Updated compilation with id: {}", compId);
        return toDto(compilation);
    }

    // --- Вспомогательные методы ---

    private Compilation getCompilationEntity(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Compilation not found with id: " + compId));
    }

    private CompilationDto toDto(Compilation compilation) {
        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(event -> EventShortDto.builder()
                        .id(event.getId())
                        .annotation(event.getAnnotation())
                        .title(event.getTitle())
                        .eventDate(event.getEventDate())
                        .paid(event.getPaid())
                        .views(event.getViews() != null ? event.getViews() : 0L)
                        .build())
                .collect(Collectors.toList());

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }
}