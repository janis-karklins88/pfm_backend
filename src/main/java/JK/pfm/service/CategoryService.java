package JK.pfm.service;

import JK.pfm.dto.CategoryListDto;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserCategoryPreferenceRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.SecurityUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            UserCategoryPreferenceRepository userCategoryPreferenceRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.userCategoryPreferenceRepository = userCategoryPreferenceRepository;
    }

    //getting all active categories for user
    public List<Category> getAllCategoriesForUser() {
        List<UserCategoryPreference> preferences = userCategoryPreferenceRepository.findByUserIdAndActiveTrue(SecurityUtil.getUserId());
        return preferences.stream()
                      .map(UserCategoryPreference::getCategory)
                      .collect(Collectors.toList());
        
    }
    
    //getting all categories for user
    public List<CategoryListDto> getAllCategories() {        
        return userCategoryPreferenceRepository.findCategoryListDtoByUserId(SecurityUtil.getUserId());
    }

    //saving category
    @Transactional
    public Category saveCategory(String name) {
        if(name.equals("Savings") || name.equals("Opening Account") || name.equals("Fund Transfer")){
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Choose different name"
            );
        }
        
        User user = SecurityUtil.getUser(userRepository);
        if (userCategoryPreferenceRepository.existsByUserAndCategory_NameIgnoreCase(user, name)) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Category already exists"
            );
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
                        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category not found"
            ));
        pref.setActive(active);
        userCategoryPreferenceRepository.save(pref);
    }
}
