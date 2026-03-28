package com.example.service;

import com.example.mapper.CategoryMapper;
import com.example.models.Category;
import com.example.repository.CategoryRepository;
import com.example.dto.CategoryRequest;
import com.example.dto.CategoryResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @CacheEvict(value = "categoriesList", allEntries = true)
    @Transactional
    public Integer createCategory(CategoryRequest request) {
        var category = mapper.toCategory(request);
        return repository.save(category).getId();
    }

    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse findById(Integer id) {
        return repository.findById(id)
                .map(mapper::toCategoryResponse)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID:: " + id));
    }

    @Cacheable(value = "categoriesList")
    public List<CategoryResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toCategoryResponse)
                .toList();
    }

    @CacheEvict(value = {"categories", "categoriesList"}, allEntries = true)
    @Transactional
    public void updateCategory(Integer id, CategoryRequest request) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID:: " + id));
        category.setName(request.name());
        category.setDescription(request.description());
        repository.save(category);
    }

    @CacheEvict(value = {"categories", "categoriesList"}, allEntries = true)
    @Transactional
    public void deleteCategory(Integer id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Category not found with ID:: " + id);
        }
        repository.deleteById(id);
    }
}

