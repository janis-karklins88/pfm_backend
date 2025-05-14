package JK.pfm.service;

import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.changePasswordRequestDTO;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.model.UserSettings;
import JK.pfm.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserSettingsRepository settingsRepository;
      
    
    //saving users
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
    
    //change username
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
    
    //change password
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

