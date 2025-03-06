package JK.pfm.service;

import java.util.Optional;
import JK.pfm.model.User;
import JK.pfm.util.Validations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import JK.pfm.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    
    //saving users
    public User saveUser(User user) {
        
        //check if username free/taken
        if (userRepository.existsByUsername(user.getUsername())) {
        throw new RuntimeException("Username already taken");
    }   
        //check for empty password and username
        Validations.emptyFieldValidation(user.getPassword(), "Password");
        Validations.emptyFieldValidation(user.getUsername(), "Username");
        
        //hashing password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}

