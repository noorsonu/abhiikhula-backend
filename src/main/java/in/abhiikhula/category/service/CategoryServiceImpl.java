package in.abhiikhula.category.service;

import in.abhiikhula.category.dto.CategoryResponseDto;
import in.abhiikhula.category.dto.CreateCategoryRequest;
import in.abhiikhula.category.mapper.CategoryMapper;
import in.abhiikhula.category.model.Category;
import in.abhiikhula.category.repository.CategoryRepository;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllActiveCategories() {
        log.debug("Fetching all active categories");
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(UUID id) {
        log.debug("Fetching category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryBySlug(String slug) {
        log.debug("Fetching category with slug: {}", slug);
        Category category = categoryRepository.findBySlug(slug.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CreateCategoryRequest request) {
        String slug = request.getSlug().trim().toLowerCase();
        log.info("Creating category: {} with slug: {}", request.getName(), slug);
        if (categoryRepository.existsBySlug(slug)) {
            throw new in.abhiikhula.common.exception.DuplicateResourceException("Category", "slug", slug);
        }
        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }
}

