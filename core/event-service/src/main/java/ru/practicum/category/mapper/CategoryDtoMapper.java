package ru.practicum.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import ru.practicum.category.model.Category;
import ru.practicum.category_service.dto.CategoryDto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryDtoMapper {

    CategoryDtoMapper INSTANCE = Mappers.getMapper(CategoryDtoMapper.class);

    CategoryDto mapCategoryToDto(Category category);

    Category mapDtoToCategory(CategoryDto categoryDto);

    List<CategoryDto> mapCatListToDtoList(List<Category> categories);
}