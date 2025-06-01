package ru.practicum.compilations.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.events.dto.EventShortDto;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    CompilationMapper INSTANCE = Mappers.getMapper(CompilationMapper.class);

    @Mapping(target = "events", source = "events") // events передается как параметр
    CompilationDto mapCompilationToCompilationDto(Compilation compilation, List<EventShortDto> events);

    @Mapping(target = "events", source = "compilation.events", qualifiedByName = "mapEventIdsToEventShortDtos")
    CompilationDto mapCompilationToCompilationDtoWithEvents(Compilation compilation, @Context HashMap<Long, EventShortDto> events);

    @Mapping(target = "events", source = "compilations.events", qualifiedByName = "mapEventIdsToEventShortDtos")
    List<CompilationDto> mapToListCompilationDto(List<Compilation> compilations, @Context HashMap<Long, EventShortDto> events);

    @Named("mapEventIdsToEventShortDtos")
    default List<EventShortDto> mapEventIdsToEventShortDtos(Set<Long> eventIds, @Context HashMap<Long, EventShortDto> events) {
        return eventIds.stream()
                .map(events::get)
                .collect(Collectors.toList());
    }
}