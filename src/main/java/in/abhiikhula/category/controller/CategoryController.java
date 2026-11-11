package in.abhiikhula.category.controller;

import in.abhiikhula.category.dto.CategoryResponseDto;
import in.abhiikhula.category.dto.CreateCategoryRequest;
import in.abhiikhula.category.service.CategoryService;
import in.abhiikhula.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "M1 Discovery - Categories", description = "Vertical Categories API (BRD FR-103, Section 30)")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all active categories", description = "Returns business verticals for chips filter (e.g. Salon, Medical)")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllActiveCategories() {
        List<CategoryResponseDto> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories, "Categories fetched successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Returns details of a specific category")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategoryById(@PathVariable UUID id) {
        CategoryResponseDto category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.ok(category));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Returns details of a category by unique slug")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponseDto category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(category));
    }

    @PostMapping
    @Operation(summary = "Create category", description = "Admin endpoint to register a new vertical (BRD AD-05)")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponseDto category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(category, "Category created successfully"));
    }
}

