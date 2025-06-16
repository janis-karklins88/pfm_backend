package JK.pfm.controller;  // <-- match your real package!

import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.ChangeUsernameDto;
import JK.pfm.dto.UserLoginRequest;
import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.changePasswordRequestDTO;
import JK.pfm.model.User;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.service.UserService;
import JK.pfm.repository.UserRepository;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.util.JWTUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.endsWith;


import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean 
    private CustomUserDetailsService customUserDetailsService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_returnsCreatedUser() throws Exception {
        var dto = new UserRegistrationDto("new-user", "pass1234");
        var saved = new User();
        saved.setId(1L);
        saved.setUsername("new-user");

        when(userService.saveUser(any(UserRegistrationDto.class)))
            .thenReturn(saved);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            // Location header should point to /api/users/register/{id}
            .andExpect(header().string("Location", endsWith("/api/users/register/1")))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("new-user"))
                .andDo(print())
            ;
    }

    @Test
    void loginUser_returnsBearerToken() throws Exception {
        var req = new UserLoginRequest("test-user", "test-pass");
        when(userService.login("test-user", "test-pass")).thenReturn("abc123");

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(content().string("Bearer abc123"));
    }

    @Test
    void getName_returnsCurrentUsername() throws Exception {
        // Prepare a fake authenticated principal
        CustomUserDetails principal = new CustomUserDetails(42L, "current-user", "secret");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        // Mock repository lookup
        var user = new User();
        user.setId(42L);
        user.setUsername("current-user");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/name"))
            .andExpect(status().isOk())
            .andExpect(content().string("current-user"));
    }

    @Test
    void changeUsername_returnsNewToken() throws Exception {
        var dto = new ChangeUsernameDto("updated-user");
        when(userService.changeUsername("updated-user")).thenReturn("newToken123");

        mockMvc.perform(patch("/api/users/change-username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("newToken123"));
    }

    @Test
    void changePassword_returnsNoContent() throws Exception {
        var dto = new changePasswordRequestDTO("oldPass", "newPass123", "newPass123");

        mockMvc.perform(patch("/api/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());

        // Verify service was invoked
        verify(userService).changePassword(
        any(changePasswordRequestDTO.class)
        );
    }
}
