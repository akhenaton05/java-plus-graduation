package ru.practicum.compilations.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.Filter;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.mapper.NewCompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.service.PublicEventsServiceImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final PublicEventsServiceImpl publicEventsService;
    private final EventMapper eventMapper;
    private final CompilationMapper compilationMapper;
    private final NewCompilationMapper newCompilationMapper;

    @Override
    public CompilationDto getById(Long compId) {
        log.info("Get compilation with id {}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Compilation with id=" + compId +
                        " was not found"));
        return compilationMapper.mapCompilationToCompilationDto(compilation, getEventsListForDto(compilation));
    }

    @Override
    @Transactional
    public CompilationDto add(NewCompilationDto newCompilationDto) {
        log.info("Add compilation {}", newCompilationDto);
        Compilation newCompilation = compilationRepository
                .save(newCompilationMapper.mapNewCompilationDtoToCompilation(newCompilationDto));
        return compilationMapper.mapCompilationToCompilationDto(newCompilation, getEventsListForDto(newCompilation));
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        log.info("Update compilation with id {}", compId);
        Compilation compilation = getCompilation(compId);

        if (Objects.nonNull(updateCompilationRequest.getEvents())) {
            compilation.setEvents(updateCompilationRequest.getEvents());
        }

        if (Objects.nonNull(updateCompilationRequest.getPinned())) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (Objects.nonNull(updateCompilationRequest.getTitle())) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }
        Compilation updateCompilation = compilationRepository.save(compilation);
        return compilationMapper.mapCompilationToCompilationDto(updateCompilation, getEventsListForDto(updateCompilation));
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        log.info("Delete compilation with id {}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new EntityNotFoundException("Compilation with id=" + compId +
                    " was not found");
        }

        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> get(Filter params) {
        log.info("Get compilations with filter {}", params);
        int page = params.getFrom() / params.getSize();
        Pageable pageable = PageRequest.of(page, params.getSize());
        Page<Compilation> response = params.getPinned() ? compilationRepository.findAllByPinnedTrue(pageable) :
                compilationRepository.findAll(pageable);
        List<Compilation> compilations = response.getContent().stream().toList();
        return compilationMapper.mapToListCompilationDto(compilations, getEventShortDtoForListDto(compilations));
    }


    //вспомогательные методы
    private Compilation getCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new EntityNotFoundException("Compilation with id=" + compId +
                        " was not found"));
    }

    private HashMap<Long, EventShortDto> getEventShortDtoForListDto(List<Compilation> compilations) {
        List<Long> eventsId = compilations.stream()
                .map(compilation -> compilation.getEvents().stream().toList())
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        //спорный способ вернуть HashMap
        return new HashMap<>(publicEventsService.getEventsByListIds(eventsId).stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toMap(EventShortDto::getId, Function.identity())));
    }

    private List<EventShortDto> getEventsListForDto(Compilation compilation) {
        if (Objects.isNull(compilation.getEvents()) || compilation.getEvents().isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<EventShortDto> events = publicEventsService.getEventsByListIds(compilation.getEvents().stream().toList())
                .stream().map(eventMapper::toEventShortDto).collect(Collectors.toList());
        log.info("EventsShortDto: {}", events);
        return events;
    }

}
