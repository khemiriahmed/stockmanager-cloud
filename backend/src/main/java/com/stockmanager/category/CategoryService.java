package com.stockmanager.category;

import com.stockmanager.category.dto.CategoryDto;
import com.stockmanager.category.dto.CategoryRequest;
import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryDto> list(String search, Boolean active, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(categoryRepository.search(query, active, pageable), CategoryDto::from);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream().map(CategoryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto get(Long id) {
        return CategoryDto.from(find(id));
    }

    public CategoryDto create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw BusinessException.conflict("Une catégorie porte déjà ce nom.");
        }
        return CategoryDto.from(categoryRepository.save(new Category(name, request.description())));
    }

    public CategoryDto update(Long id, CategoryRequest request) {
        Category category = find(id);
        String name = request.name().trim();
        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw BusinessException.conflict("Une catégorie porte déjà ce nom.");
        }
        category.setName(name);
        category.setDescription(request.description());
        return CategoryDto.from(category);
    }

    public CategoryDto setStatus(Long id, boolean active) {
        Category category = find(id);
        category.setActive(active);
        return CategoryDto.from(category);
    }

    public void deactivate(Long id) {
        Category category = find(id);
        category.setActive(false);
    }

    public Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Catégorie introuvable."));
    }
}
