package JK.pfm.controller;

import JK.pfm.model.Category;
import JK.pfm.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;
    
    //Get all categories for user
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategoriesForUser() {
        List<Category> categories = categoryService.getAllCategoriesForUser();
        return ResponseEntity.ok(categories);
    }

    //Create category
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        Category savedCategory = categoryService.saveCategory(name);
        return ResponseEntity.ok(savedCategory);
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
        Optional<Category> categoryOpt = categoryService.updateCategoryVisibility(id, active);
        return categoryOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
