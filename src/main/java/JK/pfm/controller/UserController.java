package jk.pfm.controller;

import JK.pfm.dto.ChangeUsernameDto;
import JK.pfm.dto.UserDto;
import JK.pfm.dto.UserLoginRequest;
import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.changePasswordRequestDTO;
import JK.pfm.model.User;
import JK.pfm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import JK.pfm.service.UserService;
import JK.pfm.util.SecurityUtil;
import java.util.Map;
import jakarta.validation.*;
import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    
      
    // Register new user
    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegistrationDto dto) {
        User user = userService.saveUser(dto);
        
        URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()                
        .path("/{id}")
        .buildAndExpand(user.getId())     
        .toUri();
        
        UserDto body = new UserDto(user.getId(), user.getUsername());
        return ResponseEntity
        .created(location)
        .body(body);
    }

    // Login user
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody UserLoginRequest request) {
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
    public ResponseEntity<Map<String, String>> changeName(@Valid @RequestBody ChangeUsernameDto request){
        String token = userService.changeUsername(request.getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }
    
    //change password
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody changePasswordRequestDTO request){
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
    
    
}
