package JK.pfm.controller;  // <-- match your real package!

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.ChangeAccountNameDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.model.Account;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.AccountService;
import JK.pfm.util.JWTUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;



import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean
    private AccountService accountService;

    // mock SecurityUtil for @PreAuthorize
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

    @Test
    void getAccountsForUser_returnsList() throws Exception {
        // Simulate authenticated user with ID = 42
        CustomUserDetails principal = new CustomUserDetails(42L, "user", "pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // prepare fake accounts
        Account a1 = new Account(); a1.setId(1L); a1.setName("Checking"); a1.setAmount(new BigDecimal("100"));
        Account a2 = new Account(); a2.setId(2L); a2.setName("Savings");  a2.setAmount(new BigDecimal("200"));
        when(accountService.getAccountsForUser(42L)).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",            org.hamcrest.Matchers.hasSize(2)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].name").value("Savings"));

        verify(accountService).getAccountsForUser(42L);
    }

    @Test
    void getTotalBalance_returnsSum() throws Exception {
        when(accountService.getTotalBalance()).thenReturn(new BigDecimal("350.75"));

        mockMvc.perform(get("/api/accounts/total-balance"))
            .andExpect(status().isOk())
            .andExpect(content().string("350.75"));
    }

    @Test
    void createAccount_returnsCreated() throws Exception {
        var req = new AccountCreationRequest("Investment", new BigDecimal("500"));
        Account saved = new Account(); saved.setId(7L); saved.setName("Investment"); saved.setAmount(new BigDecimal("500"));

        when(accountService.saveAccount(any(AccountCreationRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/accounts/7")))
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.name").value("Investment"));
    }

    @Test
    void deleteAccount_returnsNoContent() throws Exception {
        // no stub needed for void
        mockMvc.perform(delete("/api/accounts/13"))
            .andExpect(status().isNoContent());

        verify(accountService).deleteAccount(13L);
    }

    @Test
    void updateAccountName_returnsUpdatedAccount() throws Exception {
        // stub PreAuthorize check
        when(securityUtil.isCurrentUserAccount(5L)).thenReturn(true);

        ChangeAccountNameDto dto = new ChangeAccountNameDto("NewName");
        Account updated = new Account(); updated.setId(5L); updated.setName("NewName");

        when(accountService.updateAccountName(eq(5L), any(ChangeAccountNameDto.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/accounts/5/name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void transferFunds_returnsUpdatedAccount() throws Exception {
        SavingsFundTransferDTO dto = new SavingsFundTransferDTO( new BigDecimal("100"), "Deposit", "test-acc" );
        Account result = new Account(); result.setId(3L); result.setAmount(new BigDecimal("900"));

        when(accountService.transferAccountFunds(eq(3L), any(SavingsFundTransferDTO.class))).thenReturn(result);

        mockMvc.perform(patch("/api/accounts/3/transfer-funds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.amount").value(900));
    }
}
