package JK.pfm.bootstrap;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import JK.pfm.model.User;
import JK.pfm.model.Category;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.repository.UserRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserCategoryPreferenceRepository;


/**
 * Ensures that every {@link User} has a {@link UserCategoryPreference} for each default {@link Category}.
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Finds all users and all categories marked {@code isDefault=true}.</li>
 *   <li>For each missing (user, category) pair, creates a {@link UserCategoryPreference}.</li>
 *   <li>Idempotent: checks existence before insert.</li>
 * </ul>
 *
 * <p><strong>Ordering:</strong> Should run after {@link SystemCategoryInitializer} so default categories exist.
 * If needed, annotate with {@code @Order} or separate by profiles.</p>
 *
 * <p><strong>Performance note:</strong> For large datasets consider batching or repository methods that
 * fetch existing mappings in bulk to reduce per-row existence checks.</p>
 */
//@Component
public class UserCategoryPreferenceInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserCategoryPreferenceRepository userCategoryPreferenceRepository;
    
    @Override

    public void run(String... args) throws Exception {
        List<User> users = userRepository.findAll();
        List<Category> defaultCategories = categoryRepository.findByIsDefaultTrue();
        
        for (User user : users) {
            for (Category category : defaultCategories) {
                if (!userCategoryPreferenceRepository.existsByUserIdAndCategoryId(user.getId(), category.getId())) {
                    UserCategoryPreference pref = new UserCategoryPreference(user, category);
                    userCategoryPreferenceRepository.save(pref);
                }
            }
        }
    }
}
