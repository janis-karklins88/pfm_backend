package jk.pfm.controller;

import JK.pfm.dto.UserLoginRequest;
import JK.pfm.dto.changePasswordRequestDTO;
import JK.pfm.model.User;
import JK.pfm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import JK.pfm.service.UserService;
import JK.pfm.util.SecurityUtil;


@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    
      
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
    public ResponseEntity<String> loginUser(@RequestBody UserLoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("Bearer " + token);
    }
    
    //get username
    @GetMapping("/name")
    public ResponseEntity<String> getName(){
        User user = SecurityUtil.getUser(userRepository);
        return ResponseEntity.ok(user.getUsername());
    }
    
    //change username
    @PatchMapping("/change-username")
    public ResponseEntity<String> changeName(@RequestBody String username){
        return ResponseEntity.ok(userService.changeUsername(username));
    }
    
    //change password
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody changePasswordRequestDTO request){
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
    
    
}
