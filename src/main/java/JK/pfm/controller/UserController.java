package JK.pfm.controller;

import JK.pfm.dto.ChangeUsernameDto;
import JK.pfm.dto.UserDto;
import JK.pfm.dto.UserLoginRequest;
import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.changePasswordRequestDTO;
import JK.pfm.model.User;
import JK.pfm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(
            UserService userService,
            UserRepository userRepository
    ) {
        this.userService     = userService;
        this.userRepository  = userRepository;
    }
    
      
    /**
     * Registers a new user account.
     *
     * <p>Creates a new {@link User} from the provided registration details and returns
     * a {@link UserDto} containing the user's ID and username.</p>
     *
     * <p>Responds with {@code 201 Created} and sets the {@code Location} header to
     * {@code /api/users/{id}}.</p>
     *
     * @param dto a {@link UserRegistrationDto} containing registration information
     * @return {@code ResponseEntity} containing the created {@link UserDto}
     * @implNote Delegates to {@link UserService#saveUser(UserRegistrationDto)}.
     */
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

    /**
     * Authenticates a user and issues a JWT token.
     *
     * <p>Responds with {@code 200 OK} and a bearer token string in the response body.</p>
     *
     * @param request a {@link UserLoginRequest} containing username and password
     * @return {@code ResponseEntity} containing a JWT token prefixed with {@code "Bearer "}
     * @implNote Delegates to {@link UserService#login(String, String)}.
     */
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody UserLoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("Bearer " + token);
    }
    
    /**
     * Retrieves the username of the currently authenticated user.
     *
     * <p>Responds with {@code 200 OK} and the username as a plain string.</p>
     *
     * @return {@code ResponseEntity} containing the current user's username
     * @implNote Uses {@link SecurityUtil#getUser(UserRepository)} to access authentication context.
     */
    @GetMapping("/name")
    public ResponseEntity<String> getName(){
        User user = SecurityUtil.getUser(userRepository);
        return ResponseEntity.ok(user.getUsername());
    }
    
    /**
     * Updates the username of the authenticated user.
     *
     * <p>Responds with {@code 200 OK} and a new JWT token under the {@code "token"} key,
     * since the username change invalidates the previous token.</p>
     *
     * @param request a {@link ChangeUsernameDto} containing the new username
     * @return {@code ResponseEntity} containing a map with a new {@code token}
     * @implNote Delegates to {@link UserService#changeUsername(String)} which reissues a JWT.
     */
    @PatchMapping("/change-username")
    public ResponseEntity<Map<String, String>> changeName(@Valid @RequestBody ChangeUsernameDto request){
        String token = userService.changeUsername(request.getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }
    
    /**
     * Changes the password of the authenticated user.
     *
     * <p>Responds with {@code 204 No Content} if the password update succeeds.</p>
     *
     * @param request a {@link changePasswordRequestDTO} containing the old and new passwords
     * @return an empty {@code ResponseEntity} with {@code 204 No Content} status
     * @implNote Delegates to {@link UserService#changePassword(changePasswordRequestDTO)}.
     */
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody changePasswordRequestDTO request){
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
    
    
}
