package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.CategoryDtoOut;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.client.EventClient;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventClient eventClient;

    @Override
    public Collection<CategoryDtoOut> getAll(Integer offset, Integer limit) {
        Collection<Category> categories = categoryRepository.findWithOffsetAndLimit(offset, limit);
        return categories.stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryDtoOut get(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));

        return CategoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDtoOut add(CategoryDto categoryDto) {
        Category category = CategoryMapper.fromDto(categoryDto);
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDtoOut update(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category", id));

        category.setName(categoryDto.getName());
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toDto(saved);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Category", id);
        }

        if (Boolean.TRUE.equals(eventClient.existsByCategoryId(id))) {
            throw new IllegalStateException("Cannot delete category. There are events associated with it.");
        }

        categoryRepository.deleteById(id);
    }
}
