package JK.pfm.controller;  // <-- match your real package!

import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.dto.UserSettingsDto;
import JK.pfm.model.Transaction;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.TransactionService;
import JK.pfm.service.UserSettingsService;
import JK.pfm.util.JWTUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;





import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = UserSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean UserSettingsService userSettingsService;

    // mock @PreAuthorize
    @MockitoBean
    private SecurityUtil securityUtil;
    @MockitoBean
    private JWTUtil jwtUtil;
    @MockitoBean 
    private CustomUserDetailsService customUserDetailsService;

    @AfterEach
    void tearDown() {
        // clean up auth between tests
        SecurityContextHolder.clearContext();
    }

    // GET /api/users/settings
@Test
void getUserSettings_returnsDto() throws Exception {
    UserSettingsDto dto = new UserSettingsDto("USD");
    when(userSettingsService.getUserSettings()).thenReturn(dto);

    mockMvc.perform(get("/api/users/settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("USD"));

    verify(userSettingsService).getUserSettings();
}

// PATCH /api/users/settings/currency – happy path
@Test
void setUserCurrency_returnsNoContent() throws Exception {
    Map<String, String> req = Map.of("currency", "EUR");

    mockMvc.perform(patch("/api/users/settings/currency")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNoContent());

    verify(userSettingsService).setUserCurrency("EUR");
}

   
}
