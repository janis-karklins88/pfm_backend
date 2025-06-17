package JK.pfm.controller;  // <-- match your real package!

import JK.pfm.dto.CategoryListDto;
import JK.pfm.dto.CategoryNameDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.model.Category;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.CategoryService;
import JK.pfm.util.JWTUtil;
import JK.pfm.util.SecurityUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.endsWith;

import static org.mockito.ArgumentMatchers.eq;



import java.util.List;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean
    private CategoryService categoryService;

    // mock SecurityUtil for @PreAuthorize
    @MockitoBean
    private SecurityUtil securityUtil;
    
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean 
    private CustomUserDetailsService customUserDetailsService;

    @AfterEach
    void tearDown() {
        // clean up auth between tests
        SecurityContextHolder.clearContext();
    }

    // GET /api/categories
@Test
void getAllCategoriesForUser_returnsActiveList() throws Exception {
    Category c1 = new Category(); c1.setId(1L); c1.setName("Food");
    Category c2 = new Category(); c2.setId(2L); c2.setName("Rent");
    when(categoryService.getAllCategoriesForUser()).thenReturn(List.of(c1, c2));

    mockMvc.perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].name").value("Rent"));

    verify(categoryService).getAllCategoriesForUser();
}

// GET /api/categories/all
@Test
void getAllCategories_returnsFullList() throws Exception {
    CategoryListDto d1 = new CategoryListDto(1L, "Food", true);
    CategoryListDto d2 = new CategoryListDto(2L, "Utilities", false);
    when(categoryService.getAllCategories()).thenReturn(List.of(d1, d2));

    mockMvc.perform(get("/api/categories/all"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].active").value(false));

    verify(categoryService).getAllCategories();
}

// POST /api/categories
@Test
void createCategory_returnsCreated() throws Exception {
    CategoryNameDto req = new CategoryNameDto("NewCat");
    Category saved = new Category(); saved.setId(5L); saved.setName("NewCat");
    when(categoryService.saveCategory(eq("NewCat"))).thenReturn(saved);

    mockMvc.perform(post("/api/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/categories/5")))
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.name").value("NewCat"));

    verify(categoryService).saveCategory("NewCat");
}

// PATCH /api/categories/{id} – success
@Test
void updateCategoryVisibility_returnsNoContent_whenActiveProvided() throws Exception {
    mockMvc.perform(patch("/api/categories/3")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":false}"))
        .andExpect(status().isNoContent());

    verify(categoryService).updateCategoryVisibility(3L, false);
}

// PATCH /api/categories/{id} – missing “active” key
@Test
void updateCategoryVisibility_returnsBadRequest_whenActiveMissing() throws Exception {
    mockMvc.perform(patch("/api/categories/3")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());

    verify(categoryService, never()).updateCategoryVisibility(anyLong(), anyBoolean());
}

    
}
