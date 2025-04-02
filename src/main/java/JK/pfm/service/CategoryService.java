package JK.pfm.service;

import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserCategoryPreferenceRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    //getting all categories for user
    public List<Category> getAllCategoriesForUser() {
        User user = SecurityUtil.getUser(userRepository);
        
        List<UserCategoryPreference> preferences = userCategoryPreferenceRepository.findByUserIdAndActiveTrue(user.getId());
    
        return preferences.stream()
                      .map(UserCategoryPreference::getCategory)
                      .collect(Collectors.toList());
        
    }

    //saving category
    public Category saveCategory(String name) {
        
        Validations.emptyFieldValidation(name, "name");
        
        Optional<Category> duplicateCategory = categoryRepository.findByName(name);
        if(!duplicateCategory.isEmpty()){
            throw new RuntimeException("Category already exists");
        }
        
        Category category = new Category(name);
        categoryRepository.save(category);
        
        User user = SecurityUtil.getUser(userRepository);
        UserCategoryPreference pref = new UserCategoryPreference(user, category);
        user.addCategoryPreference(pref);       
        
        return category;
    }


    //set active/inactive
    public Optional<Category> updateCategoryVisibility(Long categoryId, boolean active) {
        // Get the current user
        User user = SecurityUtil.getUser(userRepository);
    
        // Find the preference record for this user and category
        Optional<UserCategoryPreference> prefOpt = userCategoryPreferenceRepository.findByUserIdAndCategoryId(user.getId(), categoryId);
    
        if (prefOpt.isEmpty()) {
            throw new RuntimeException("User preference for category not found");
        }
    
        UserCategoryPreference pref = prefOpt.get();
        pref.setActive(active);
        userCategoryPreferenceRepository.save(pref);
    
        return Optional.of(pref.getCategory());
    }
}
