package JK.demo.service;

import java.util.Optional;
import JK.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import JK.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    
    
    public User saveUser(User user) {
        //hashing password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //check if username free/taken
        if (userRepository.existsByUsername(user.getUsername())) {
        throw new RuntimeException("Username already taken");
    }
        return userRepository.save(user);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}

