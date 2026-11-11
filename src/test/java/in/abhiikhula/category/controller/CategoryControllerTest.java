package in.abhiikhula.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.abhiikhula.category.dto.CategoryResponseDto;
import in.abhiikhula.category.dto.CreateCategoryRequest;
import in.abhiikhula.category.service.CategoryService;
import in.abhiikhula.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("GET /api/categories returns active categories")
    void shouldReturnActiveCategories() throws Exception {
        CategoryResponseDto salon = CategoryResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Salon & Barber")
                .slug("salon-barber")
                .icon("scissors")
                .defaultServiceMin(15)
                .active(true)
                .createdAt(Instant.now())
                .build();

        CategoryResponseDto medical = CategoryResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Medical & Pharmacy")
                .slug("medical-pharmacy")
                .icon("pill")
                .defaultServiceMin(5)
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(categoryService.getAllActiveCategories()).thenReturn(List.of(salon, medical));

        mockMvc.perform(get("/api/categories").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].slug").value("salon-barber"))
                .andExpect(jsonPath("$.data[1].defaultServiceMin").value(5));
    }

    @Test
    @DisplayName("GET /api/categories/slug/{slug} returns category")
    void shouldReturnCategoryBySlug() throws Exception {
        CategoryResponseDto dto = CategoryResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Medical & Pharmacy")
                .slug("medical-pharmacy")
                .icon("pill")
                .defaultServiceMin(5)
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(categoryService.getCategoryBySlug("medical-pharmacy")).thenReturn(dto);

        mockMvc.perform(get("/api/categories/slug/medical-pharmacy").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Medical & Pharmacy"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} returns 404 when not found")
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(categoryService.getCategoryById(randomId))
                .thenThrow(new ResourceNotFoundException("Category", "id", randomId));

        mockMvc.perform(get("/api/categories/{id}", randomId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/categories creates category and returns 201")
    void shouldCreateCategory() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Clinic & Doctor")
                .slug("clinic-doctor")
                .icon("stethoscope")
                .defaultServiceMin(10)
                .active(true)
                .build();

        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .slug(request.getSlug())
                .icon(request.getIcon())
                .defaultServiceMin(request.getDefaultServiceMin())
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("clinic-doctor"));
    }

    @Test
    @DisplayName("POST /api/categories returns 400 when name is blank")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("")
                .slug("invalid-slug")
                .build();

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

