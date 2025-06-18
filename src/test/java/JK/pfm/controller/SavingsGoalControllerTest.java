package JK.pfm.controller;  // <-- match your real package!

import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.UpdateSavingsAmountDto;
import JK.pfm.model.SavingsGoal;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.SavingsGoalService;
import JK.pfm.util.JWTUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.endsWith;




import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = SavingsGoalController.class)
@AutoConfigureMockMvc(addFilters = false)
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean SavingsGoalService savingsGoalService;

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

    // GET /api/savings-goals
@Test
void getAllSavingsGoals_returnsList() throws Exception {
    // simulate authenticated user id=42
    CustomUserDetails principal = new CustomUserDetails(42L, "user", "pw");
    Authentication auth = new UsernamePasswordAuthenticationToken(principal, null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    SavingsGoal g1 = new SavingsGoal(); g1.setId(1L);
    SavingsGoal g2 = new SavingsGoal(); g2.setId(2L);
    when(savingsGoalService.getAllSavingsGoals(42L)).thenReturn(List.of(g1, g2));

    mockMvc.perform(get("/api/savings-goals"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(2));

    verify(savingsGoalService).getAllSavingsGoals(42L);
}

// GET /api/savings-goals/savings-balance
@Test
void getTotalSavingsBalance_returnsValue() throws Exception {
    when(savingsGoalService.getTotalBalance()).thenReturn(new BigDecimal("123.45"));

    mockMvc.perform(get("/api/savings-goals/savings-balance"))
        .andExpect(status().isOk())
        .andExpect(content().string("123.45"));

    verify(savingsGoalService).getTotalBalance();
}

// POST /api/savings-goals
@Test
void createSavingsGoal_returnsCreated() throws Exception {
    SavingGoalCreation req = new SavingGoalCreation(
        new BigDecimal("1000"), "MyGoal", "Desc"
    );
    SavingsGoal saved = new SavingsGoal(); saved.setId(7L);
    when(savingsGoalService.saveSavingsGoal(any(SavingGoalCreation.class)))
        .thenReturn(saved);

    mockMvc.perform(post("/api/savings-goals")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/savings-goals/7")))
        .andExpect(jsonPath("$.id").value(7));

    verify(savingsGoalService).saveSavingsGoal(any(SavingGoalCreation.class));
}

// GET /api/savings-goals/{id} – found
@Test
void getSavingsGoalById_returnsOkWhenFound() throws Exception {
    SavingsGoal goal = new SavingsGoal(); goal.setId(5L);
    when(savingsGoalService.getSavingsGoalById(5L))
        .thenReturn(Optional.of(goal));

    mockMvc.perform(get("/api/savings-goals/5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5));

    verify(savingsGoalService).getSavingsGoalById(5L);
}

// GET /api/savings-goals/{id} – not found
@Test
void getSavingsGoalById_returnsNotFoundWhenMissing() throws Exception {
    when(savingsGoalService.getSavingsGoalById(99L))
        .thenReturn(Optional.empty());

    mockMvc.perform(get("/api/savings-goals/99"))
        .andExpect(status().isNotFound());

    verify(savingsGoalService).getSavingsGoalById(99L);
}

// DELETE /api/savings-goals/{id}
@Test
void deleteSavingsGoal_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/savings-goals/3"))
        .andExpect(status().isNoContent());

    verify(savingsGoalService).deleteSavingsGoal(3L);
}

// PATCH /api/savings-goals/{id}/amount
@Test
void updateSavingGoalAmount_returnsUpdated() throws Exception {
    UpdateSavingsAmountDto dto = new UpdateSavingsAmountDto(new BigDecimal("500"));
    SavingsGoal updated = new SavingsGoal(); updated.setId(8L);
    when(savingsGoalService.updateSavingsGoalAmount(eq(8L), any(UpdateSavingsAmountDto.class)))
        .thenReturn(updated);

    mockMvc.perform(patch("/api/savings-goals/8/amount")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(8));

    verify(savingsGoalService)
        .updateSavingsGoalAmount(eq(8L), any(UpdateSavingsAmountDto.class));
}

// PATCH /api/savings-goals/{id}/transfer-funds
@Test
void transferFundsSavingsGoal_returnsUpdated() throws Exception {
    SavingsFundTransferDTO dto = new SavingsFundTransferDTO(
        new BigDecimal("200"), "deposit", "Checking"
    );
    SavingsGoal updated = new SavingsGoal(); updated.setId(9L);
    when(savingsGoalService.transferFunds(eq(9L), any(SavingsFundTransferDTO.class)))
        .thenReturn(updated);

    mockMvc.perform(patch("/api/savings-goals/9/transfer-funds")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9));

    verify(savingsGoalService)
        .transferFunds(eq(9L), any(SavingsFundTransferDTO.class));
}

// GET /api/savings-goals/net-balance
@Test
void getNetMonthlyBalance_returnsMap() throws Exception {
    Map<String, BigDecimal> map = Map.of(
        "2025-06", new BigDecimal("70.00"),
        "2025-07", new BigDecimal("100.00")
    );
    when(savingsGoalService.getNetMonthlyBalance()).thenReturn(map);

    mockMvc.perform(get("/api/savings-goals/net-balance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.['2025-06']").value(70.00))
        .andExpect(jsonPath("$.['2025-07']").value(100.00));

    verify(savingsGoalService).getNetMonthlyBalance();
}


}
