package ru.practicum.ewm.category.service;

import java.util.Collection;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.CategoryDtoOut;

public interface CategoryService {

    Collection<CategoryDtoOut> getAll(Integer offset, Integer limit);

    CategoryDtoOut get(Long id);

    CategoryDtoOut add(CategoryDto categoryDto);

    CategoryDtoOut update(Long id, CategoryDto categoryDto);

    void delete(Long id);
}
