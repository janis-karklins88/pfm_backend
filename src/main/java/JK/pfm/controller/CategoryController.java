package JK.pfm.controller;

import JK.pfm.dto.CategoryListDto;
import JK.pfm.dto.CategoryNameDto;
import JK.pfm.model.Category;
import JK.pfm.service.CategoryService;
import jakarta.validation.Valid;
import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/categories")
public class CategoryController {


    private final CategoryService categoryService;
    
    public CategoryController (CategoryService service){
        this.categoryService = service;
    }
    /**
    * Retrieves all active categories for the authenticated user.
    *
    * <p>Returns only categories that are marked as active and available
     * for selection when creating or viewing transactions.</p>
     *
     * <p>Responds with {@code 200 OK} and a list of active {@link Category} objects.</p>
     *
     * @return {@code ResponseEntity} containing the list of active categories
     * @implNote Delegates to {@link CategoryService#getAllCategoriesForUser()}.
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategoriesForUser() {
        List<Category> categories = categoryService.getAllCategoriesForUser();
        return ResponseEntity.ok(categories);
    }
    
    /**
     * Retrieves all categories for the authenticated user, including both
     * active and inactive entries.
     *
     * <p>Useful for displaying complete category lists in settings or
     * management pages where visibility toggling is possible.</p>
     *
     * <p>Responds with {@code 200 OK} and a list of {@link CategoryListDto} objects.</p>
     *
     * @return {@code ResponseEntity} containing all categories
     * @implNote Delegates to {@link CategoryService#getAllCategories()}.
     */
    @GetMapping("/all")
    public ResponseEntity<List<CategoryListDto>> getAllCategories() {
        List<CategoryListDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Creates a new category for the authenticated user.
     *
     * <p>Responds with {@code 201 Created} and the created {@link Category}
     * in the response body. The {@code Location} header of the response
     * points to {@code /api/categories/{id}}.</p>
     *
     * @param request a {@link CategoryNameDto} containing the category name
     * @return {@code ResponseEntity} containing the created {@link Category}
     * @implNote The {@link CategoryService#saveCategory(String)} method handles
     * name validation and ownership assignment.
     */
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryNameDto request) {
        var cat = categoryService.saveCategory(request.getName());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(cat.getId())
        .toUri();
        return ResponseEntity.created(uri).body(cat);
    }
    
    /**
     * Updates the visibility (active/inactive) state of a category.
     *
     * <p>Used primarily in settings to enable or disable categories without deleting them.</p>
     *
     * <p>Responds with {@code 204 No Content} if the update succeeds,
     * or {@code 400 Bad Request} if the request body is missing the
     * required {@code active} field.</p>
     *
     * @param id the ID of the category to update
     * @param request a map containing a single {@code active} key with a boolean value
     * @return {@code ResponseEntity} with {@code 204 No Content} or {@code 400 Bad Request}
     * @implNote Delegates to {@link CategoryService#updateCategoryVisibility(Long, Boolean)}.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Category> updateCategoryVisibility(
            @PathVariable Long id, 
            @RequestBody Map<String, Boolean> request) {
        Boolean active = request.get("active");
        if (active == null) {
            return ResponseEntity.badRequest().build();
        }
        categoryService.updateCategoryVisibility(id, active);
        return ResponseEntity.noContent().build();
    }
}
