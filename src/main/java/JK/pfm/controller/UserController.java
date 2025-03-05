package jk.pfm.controller;

import java.util.Optional;
import JK.pfm.model.User;
import JK.pfm.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import JK.pfm.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    // Register new user
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        try {
            userService.saveUser(user);
            return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Login user
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody User user) {
        Optional<User> existingUser = userService.getUserByUsername(user.getUsername());
        //check if user exists
        if (existingUser.isPresent()) {
            //compare passwords
            if (passwordEncoder.matches(user.getPassword(), existingUser.get().getPassword())) {
                //generating token
                String token = JWTUtil.generateToken(user.getUsername());
                
                return ResponseEntity.ok("Bearer " + token);
        }
            
        } 
        return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
        
    }
    
}
