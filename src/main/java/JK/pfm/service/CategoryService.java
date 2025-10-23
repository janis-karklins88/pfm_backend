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

    /**
     * Retrieves all active categories for the currently authenticated user.
     *
     * @return a list of active {@link JK.pfm.model.Category} entities
     */
    public List<Category> getAllCategoriesForUser() {
        List<UserCategoryPreference> preferences = userCategoryPreferenceRepository.findByUserIdAndActiveTrue(SecurityUtil.getUserId());
        return preferences.stream()
                      .map(UserCategoryPreference::getCategory)
                      .collect(Collectors.toList());
        
    }
    
    /**
     * Retrieves all categories for the currently authenticated user,
     * including their active status.
     *
     * @return a list of {@link JK.pfm.dto.CategoryListDto} containing category details and visibility flags
     */
    public List<CategoryListDto> getAllCategories() {        
        return userCategoryPreferenceRepository.findCategoryListDtoByUserId(SecurityUtil.getUserId());
    }

    /**
     * Creates a new custom category for the currently authenticated user.
     * <p>
     * Prevents creating system-reserved names such as "Savings",
     * "Opening Account", and "Fund Transfer". Links the new category
     * to the user via a {@link JK.pfm.model.UserCategoryPreference}.
     *
     * @param name the name of the new category
     * @return the created {@link JK.pfm.model.Category}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the name is reserved or already exists for the user (409 CONFLICT)
     */
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


    /**
     * Updates the visibility (active status) of a user's category.
     *
     * @param categoryId the ID of the category to update
     * @param active {@code true} to activate, {@code false} to deactivate
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the category preference is not found (404 NOT FOUND)
     */
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
