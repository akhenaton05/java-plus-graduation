package ru.practicum.compilations.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.model.Compilation;

@Mapper(componentModel = "spring")
public interface NewCompilationMapper {

    NewCompilationMapper INSTANCE = Mappers.getMapper(NewCompilationMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pinned", expression = "java(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)")
    Compilation mapNewCompilationDtoToCompilation(NewCompilationDto newCompilationDto);
}