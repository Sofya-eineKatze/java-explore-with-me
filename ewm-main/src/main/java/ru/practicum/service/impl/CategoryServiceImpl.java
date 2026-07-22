package ru.practicum.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto addCategory(CategoryDto categoryDto) {
        Category category = categoryMapper.toEntity(categoryDto);
        Category saved = categoryRepository.save(category);
        log.info("Added category with id: {}", saved.getId());
        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategoryEntity(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Cannot delete category with events");
        }

        categoryRepository.delete(category);
        log.info("Deleted category with id: {}", catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = getCategoryEntity(catId);
        category.setName(categoryDto.getName());
        Category updated = categoryRepository.save(category);
        log.info("Updated category with id: {}", catId);
        return categoryMapper.toDto(updated);
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable)
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        return categoryMapper.toDto(getCategoryEntity(catId));
    }

    private Category getCategoryEntity(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + catId));
    }
}