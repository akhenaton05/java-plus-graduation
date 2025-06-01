package ru.practicum.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;

@Mapper(componentModel = "spring")
public interface NewCategoryMapper {

    NewCategoryMapper INSTANCE = Mappers.getMapper(NewCategoryMapper.class);

    @Mapping(target = "id", ignore = true)
    Category mapNewCategoryDtoToCategory(NewCategoryDto newCategoryDto);
}