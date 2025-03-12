package JK.pfm.service;

import JK.pfm.model.Category;
import JK.pfm.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    //getting all categories
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByActiveTrue();
    }

    //saving category
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    //deleting category
    @Transactional
    public void deleteCategory(Long id) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            // Optionally check if it is in use and handle accordingly.
            category.setActive(false);
            categoryRepository.save(category);
        } else {
            throw new RuntimeException("Category not found");
        }
    }


    //getting one category
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
}
