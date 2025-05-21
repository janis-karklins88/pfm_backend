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
    //Get all active categories for user
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategoriesForUser() {
        List<Category> categories = categoryService.getAllCategoriesForUser();
        return ResponseEntity.ok(categories);
    }
    
    //Get all categories for user
    @GetMapping("/all")
    public ResponseEntity<List<CategoryListDto>> getAllCategories() {
        List<CategoryListDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    //Create category
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryNameDto request) {
        var cat = categoryService.saveCategory(request.getName());
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(cat.getId())
        .toUri();
        return ResponseEntity.created(uri).body(cat);
    }
    
    //Set active/inactive by ID, controller for settings
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
