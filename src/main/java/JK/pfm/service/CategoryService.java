package JK.pfm.service;

import JK.pfm.dto.CategoryListDto;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserCategoryPreferenceRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    //getting all active categories for user
    public List<Category> getAllCategoriesForUser() {
        User user = SecurityUtil.getUser(userRepository);
        
        List<UserCategoryPreference> preferences = userCategoryPreferenceRepository.findByUserIdAndActiveTrue(user.getId());
    
        return preferences.stream()
                      .map(UserCategoryPreference::getCategory)
                      .collect(Collectors.toList());
        
    }
    
    //getting all categories for user
    public List<CategoryListDto> getAllCategories() {
        User user = SecurityUtil.getUser(userRepository);
        
        return userCategoryPreferenceRepository.findByUserId(user.getId())
        .stream()
        .map(pref -> new CategoryListDto(
            pref.getCategory().getId(),       // supply the ID
            pref.getCategory().getName(),
            pref.getActive()
        ))
        .toList();
    }

    //saving category
    public Category saveCategory(String name) {
        Validations.emptyFieldValidation(name, "name");
        if(name.equals("Savings") || name.equals("Opening Account")){
            throw new IllegalArgumentException("Choose different name for category");
        }
        
        User user = SecurityUtil.getUser(userRepository);
        if (userCategoryPreferenceRepository.existsByUserAndCategory_NameIgnoreCase(user, name)) {
        throw new IllegalArgumentException("Category with this name already exists.");
    }
        
        Category category = new Category(name);
        categoryRepository.save(category);

        UserCategoryPreference pref = new UserCategoryPreference(user, category);
        user.addCategoryPreference(pref);       
        userRepository.save(user);
        
        return category;
    }


    //set active/inactive
    @Transactional
    public void updateCategoryVisibility(Long categoryId, boolean active) {
        UserCategoryPreference pref = 
                userCategoryPreferenceRepository.findByUserIdAndCategoryId(SecurityUtil.getUserId(), categoryId)
                        .orElseThrow(() -> new RuntimeException("User preference not found"));
        pref.setActive(active);
        userCategoryPreferenceRepository.save(pref);
    }
}
