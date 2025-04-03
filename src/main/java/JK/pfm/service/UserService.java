package JK.pfm.service;

import JK.pfm.model.Category;
import java.util.Optional;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.util.Validations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.JWTUtil;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CategoryRepository categoryRepository;
      
    
    //saving users
    public User saveUser(User user) {
    // Check if username is already taken
    if (userRepository.existsByUsername(user.getUsername())) {
        throw new RuntimeException("Username already taken");
    }   
    // Validate fields
    Validations.emptyFieldValidation(user.getPassword(), "Password");
    Validations.emptyFieldValidation(user.getUsername(), "Username");
    
    // Hash the password
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    
    // Get default categories and add preferences to the user's collection
    List<Category> defaultCategories = categoryRepository.findByIsDefaultTrue();
    for (Category category : defaultCategories) {
        UserCategoryPreference pref = new UserCategoryPreference(user, category);
        user.addCategoryPreference(pref);
    }
    
    // Save the user; cascading will save the preferences as well
    return userRepository.save(user);
}


    public String login(String username, String password) {
   
    User foundUser = userRepository.findByUsername(username)
    .orElseThrow(() -> new RuntimeException("Incorrect username!"));
    
    // Validate the password using the injected passwordEncoder
    if (!passwordEncoder.matches(password, foundUser.getPassword())) {
        throw new RuntimeException("Invalid password");
    }
    // Generate the JWT token
    return JWTUtil.generateToken(username);
}
    
    
    

}

