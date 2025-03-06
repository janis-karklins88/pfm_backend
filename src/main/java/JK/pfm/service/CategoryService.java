package JK.pfm.service;

import JK.pfm.model.Category;
import JK.pfm.repository.CategoryRepository;
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
        return categoryRepository.findAll();
    }

    //saving category
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    //deleting category
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    //getting one category
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
}
