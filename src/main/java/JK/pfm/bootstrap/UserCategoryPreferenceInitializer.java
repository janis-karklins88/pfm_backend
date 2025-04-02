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
