
package JK.pfm.service;

import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.changePasswordRequestDTO;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserSettings;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.repository.UserSettingsRepository;
import JK.pfm.service.UserService;
import JK.pfm.util.JWTUtil;
import JK.pfm.util.SecurityUtil;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceTest {
    
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CategoryRepository categoryRepository;
    @Mock UserSettingsRepository settingsRepository;
    
    @InjectMocks
    UserService userService;
    
    private UserRegistrationDto regRequest;
    private changePasswordRequestDTO changePassRequest;
    private User registredUser;
    
    @BeforeEach
    void setUp(){
        registredUser = new User ("test-user", "test-pass");
    }
    
    @Test
    void loginUser_incorrectUsername(){
        when(userRepository.findByUsername("username"))
        .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.login("username", "password"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(
        e -> ((ResponseStatusException)e).getStatusCode(),
        e -> ((ResponseStatusException)e).getReason()
        )
        .containsExactly(
        HttpStatus.UNAUTHORIZED,
        "Invalid username or password"
        );
    }
    
    @Test
    void loginUser_incorrectPassword(){
        when(userRepository.findByUsername("test-user"))
        .thenReturn(Optional.of(registredUser));
        
        when(passwordEncoder.matches("password", registredUser.getPassword()))
        .thenReturn(false);
        
        assertThatThrownBy(() -> userService.login("username", "password"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(
        e -> ((ResponseStatusException)e).getStatusCode(),
        e -> ((ResponseStatusException)e).getReason()
        )
        .containsExactly(
        HttpStatus.UNAUTHORIZED,
        "Invalid username or password"
        );
    }
    
    @Test
    void loginUser_happyPath(){
        when(userRepository.findByUsername("test-user"))
        .thenReturn(Optional.of(registredUser));
        
        when(passwordEncoder.matches("test-pass", registredUser.getPassword()))
        .thenReturn(true);
        
        try (MockedStatic<JWTUtil> jwtMock = Mockito.mockStatic(JWTUtil.class)) {
            jwtMock.when(() -> JWTUtil.generateToken("test-user"))
                   .thenReturn("fake-jwt-token");

            // 3. Act: call your service
            String token = userService.login("test-user", "test-pass");

            // 4. Assert: the service returns the stubbed token
            assertEquals("fake-jwt-token", token);

            // (Optional) verify the static was invoked
            jwtMock.verify(() -> JWTUtil.generateToken("test-user"), times(1));
        }
    }
    
    @Test
    void saveUser_usernameTaken_throwsConflict() {
    // given
    regRequest = new UserRegistrationDto("existingUser", "pass");
    when(userRepository.existsByUsername("existingUser")).thenReturn(true);

    // expect
    assertThatThrownBy(() -> userService.saveUser(regRequest))
      .isInstanceOf(ResponseStatusException.class)
      .extracting(
        e -> ((ResponseStatusException)e).getStatusCode(),
        e -> ((ResponseStatusException)e).getReason()
      )
      .containsExactly(
        HttpStatus.CONFLICT,
        "Username already taken"
      );

    // verify we never touched any of the downstream repos
    verify(userRepository).existsByUsername("existingUser");
    verify(userRepository, never()).save(any());
    verify(categoryRepository, never()).findByIsDefaultTrue();
    verify(settingsRepository, never()).save(any());
    }
    
    @Test
void saveUser_happyPath_createsUserCategoryPrefsAndSettings() {
    // given
    UserRegistrationDto dto = new UserRegistrationDto("newUser", "plainPass");

    when(userRepository.existsByUsername("newUser"))
      .thenReturn(false);
    when(passwordEncoder.encode("plainPass"))
      .thenReturn("encoded-pass");

    Category cat1 = new Category();
    cat1.setId(1L);
    cat1.setName("Default A");
    Category cat2 = new Category();
    cat2.setId(2L);
    cat2.setName("Default B");
    when(categoryRepository.findByIsDefaultTrue())
      .thenReturn(List.of(cat1, cat2));

    // echo‐back saves
    when(userRepository.save(any(User.class)))
      .thenAnswer(inv -> inv.getArgument(0));
    when(settingsRepository.save(any(UserSettings.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    // when
    User result = userService.saveUser(dto);

    // then — user fields
    assertThat(result.getUsername()).isEqualTo("newUser");
    assertThat(result.getPassword()).isEqualTo("encoded-pass");

    // then — category‐preferences added
    assertThat(result.getCategoryPreferences())
      .hasSize(2)
      .extracting(pref -> pref.getCategory())
      .containsExactlyInAnyOrder(cat1, cat2);

    // verify interactions
    verify(userRepository).existsByUsername("newUser");
    verify(userRepository).save(result);
    verify(categoryRepository).findByIsDefaultTrue();
    verify(settingsRepository).save(argThat(s ->
      s.getUser() == result &&
      "EUR".equals(s.getCurrency())
    ));
    }

    // Change Username tests

    @Test
    void changeUsername_usernameTaken_throwsConflict() {
    String newName = "takenUser";
    when(userRepository.existsByUsername(newName)).thenReturn(true);

    assertThatThrownBy(() -> userService.changeUsername(newName))
      .isInstanceOf(ResponseStatusException.class)
      .extracting(
        e -> ((ResponseStatusException)e).getStatusCode(),
        e -> ((ResponseStatusException)e).getReason()
      )
      .containsExactly(
        HttpStatus.CONFLICT,
        "Username already taken"
      );

    // should never touch the repo save or token generation
    verify(userRepository, never()).save(any());
    }

    @Test
    void changeUsername_happyPath_updatesUsernameAndReturnsToken() {
    String newName = "newUser";
    when(userRepository.existsByUsername(newName)).thenReturn(false);

    User stubUser = new User("oldUser","oldPass");
    try (MockedStatic<SecurityUtil> sec = Mockito.mockStatic(SecurityUtil.class)) {
      sec.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(stubUser);

      when(userRepository.save(any(User.class)))
        .thenAnswer(inv -> inv.getArgument(0));

      try (MockedStatic<JWTUtil> jwt = Mockito.mockStatic(JWTUtil.class)) {
        jwt.when(() -> JWTUtil.generateToken(newName))
           .thenReturn("jwt-token-123");

        String token = userService.changeUsername(newName);

        // username was updated on the returned user
        assertThat(stubUser.getUsername()).isEqualTo(newName);
        // token is what JWTUtil gave us
        assertThat(token).isEqualTo("jwt-token-123");

        verify(userRepository).save(stubUser);
        jwt.verify(() -> JWTUtil.generateToken(newName), times(1));
      }
    }
}

    // Change Password tests

    @Test
    void changePassword_newMismatch_throwsBadRequest() {
    var req = new changePasswordRequestDTO("old", "new1", "new2");

    assertThatThrownBy(() -> userService.changePassword(req))
      .isInstanceOf(ResponseStatusException.class)
      .extracting(
        e -> ((ResponseStatusException)e).getStatusCode(),
        e -> ((ResponseStatusException)e).getReason()
      )
      .containsExactly(
        HttpStatus.BAD_REQUEST,
        "New password and confirmation do not match"
      );

    // no static security call, no repo or encoder
    verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void changePassword_incorrectCurrent_throwsUnauthorized() {
    var req = new changePasswordRequestDTO("wrongOld", "newPass", "newPass");

    User stubUser = new User("user","encodedOld");
    try (MockedStatic<SecurityUtil> sec = Mockito.mockStatic(SecurityUtil.class)) {
      sec.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(stubUser);

      when(passwordEncoder.matches("wrongOld", "encodedOld"))
        .thenReturn(false);

      assertThatThrownBy(() -> userService.changePassword(req))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(
          e -> ((ResponseStatusException)e).getStatusCode(),
          e -> ((ResponseStatusException)e).getReason()
        )
        .containsExactly(
          HttpStatus.UNAUTHORIZED,
          "Current password is incorrect"
        );

      verify(userRepository, never()).save(any());
    }
    }

    @Test
    void changePassword_happyPath_updatesAndSaves() {
    var req = new changePasswordRequestDTO("oldPwd", "newPwd", "newPwd");

    User stubUser = new User("user","encodedOld");
    try (MockedStatic<SecurityUtil> sec = Mockito.mockStatic(SecurityUtil.class)) {
      sec.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(stubUser);

      when(passwordEncoder.matches("oldPwd", "encodedOld"))
        .thenReturn(true);
      when(passwordEncoder.encode("newPwd"))
        .thenReturn("encodedNew");
      when(userRepository.save(any(User.class)))
        .thenAnswer(inv -> inv.getArgument(0));

      userService.changePassword(req);

      // user object was mutated and saved
      assertThat(stubUser.getPassword()).isEqualTo("encodedNew");
      verify(userRepository).save(stubUser);
    }
    }

    
}
