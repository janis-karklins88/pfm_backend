package JK.pfm.service;

import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.changePasswordRequestDTO;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.model.UserSettings;
import JK.pfm.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import JK.pfm.repository.UserRepository;
import JK.pfm.repository.UserSettingsRepository;
import JK.pfm.util.JWTUtil;
import JK.pfm.util.SecurityUtil;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service responsible for handling user-related operations in the Personal Finance Manager application.
 * <p>
 * Provides methods for user registration, authentication, and profile management.
 * This service acts as the main layer between the controllers and the {@link JK.pfm.repository.UserRepository}.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final UserSettingsRepository settingsRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CategoryRepository categoryRepository,
            UserSettingsRepository settingsRepository
    ) {
        this.userRepository       = userRepository;
        this.passwordEncoder      = passwordEncoder;
        this.categoryRepository   = categoryRepository;
        this.settingsRepository   = settingsRepository;
    }
      
    
    /**
    * Registers a new user and initializes default resources.
    * <p>
    * Validates username uniqueness, hashes password, creates default
    * {@link JK.pfm.model.UserSettings} (with EUR as default currency), and seeds
    * {@link JK.pfm.model.UserCategoryPreference} entries for all default
    * {@link JK.pfm.model.Category} values. Runs in a single transaction.
    * 
    * @param dto the DTO containing username and password
    * @return the created {@link User} entity
    * @throws ResponseStatusException if the username is already in use
    *         This exception is handled by the global exception handler to produce a standardized error response.
    */
    @Transactional
    public User saveUser(UserRegistrationDto dto) {
    // Check if username is already taken
    if (userRepository.existsByUsername(dto.getUsername())) {
        throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Username already taken"
      );
    }   
    User user = new User();
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    user.setUsername(dto.getUsername());
    
    userRepository.save(user);
    
    // Get default categories and add preferences to the user's collection
    List<Category> defaultCategories = categoryRepository.findByIsDefaultTrue();
    for (Category category : defaultCategories) {
        UserCategoryPreference pref = new UserCategoryPreference(user, category);
        user.addCategoryPreference(pref);
    }
    //create default settings
    UserSettings settings = new UserSettings(user, "EUR");
    settingsRepository.save(settings);
    
    return user;
    
    }

    /**
    * Authenticates a user by verifying their credentials and returns a JWT token.
    *<p>
    * Validates the provided username and password using the configured
    * {@code PasswordEncoder}. If authentication succeeds, generates and returns
    * a JWT token for subsequent authorized requests.
    *
    * @param username the username of the user
    * @param password the user's raw password
    * @return a JWT token if authentication is successful
    * @throws org.springframework.web.server.ResponseStatusException
    *         if the username does not exist or the password is invalid (401 UNAUTHORIZED)
    */
    public String login(String username, String password) {
   
    User foundUser = userRepository.findByUsername(username)
    .orElseThrow(() -> new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Invalid username or password"
      ));
    
    // Validate the password using the injected passwordEncoder
    if (!passwordEncoder.matches(password, foundUser.getPassword())) {
        throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Invalid username or password"
      );
    }
    // Generate the JWT token
    return JWTUtil.generateToken(username);
}
    
    /**
    * Changes the username of the currently authenticated user.
    * <p>
    * Checks for username availability, updates the user record, and returns a
    * new JWT token reflecting the updated username.
    *
    * @param username the new username to set
    * @return a newly generated JWT token with the updated username
    * @throws org.springframework.web.server.ResponseStatusException
    *         if the username is already taken (409 CONFLICT)
    */
    @Transactional
    public String changeUsername(String username){
        if (userRepository.existsByUsername(username)) {
        throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Username already taken"
        );
        }
        
        User user = SecurityUtil.getUser(userRepository);
        user.setUsername(username);
        userRepository.save(user);
        return JWTUtil.generateToken(username);
  
    }
    
    /**
    * Changes the password of the currently authenticated user.
    * <p>
    * Verifies that the provided current password is correct and that the new
    * password matches its confirmation field before updating the record.
    *
    * @param request the DTO containing the current password, new password, and confirmation
    * @throws org.springframework.web.server.ResponseStatusException
    *         if the current password is incorrect (401 UNAUTHORIZED) or if the
    *         new password and confirmation do not match (400 BAD REQUEST)
    */

    @Transactional
    public void changePassword(changePasswordRequestDTO request){
        
        if(!request.getNewPassword().equals(request.getNewPasswordCheck())){
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "New password and confirmation do not match"
            );
        }
        
        User user = SecurityUtil.getUser(userRepository);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Current password is incorrect"
            );
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    

}

