package ru.practicum.event_service.category.service;

import ru.practicum.event_service.category.dto.CategoryDto;
import ru.practicum.event_service.category.dto.NewCategoryDto;
import ru.practicum.event_service.category.model.Category;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);

    List<CategoryDto> getCategories(Integer from, Integer size);

    CategoryDto getCategoryById(Long catId);

    Category getEntityById(Long catId);

}
