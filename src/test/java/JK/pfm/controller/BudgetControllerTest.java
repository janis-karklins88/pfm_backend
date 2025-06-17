package JK.pfm.controller;  // <-- match your real package!

import JK.pfm.dto.BudgetCreationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.UpdateBudgetAmountDto;
import JK.pfm.dto.filters.DateRangeFilter;
import JK.pfm.model.Budget;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.BudgetService;
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
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;



import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = BudgetController.class)
@AutoConfigureMockMvc(addFilters = false)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean
    private BudgetService budgetService;

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
    void getAllBudgets_returnsList() throws Exception {
    // assume your filter has start/end as query params
    DateRangeFilter filter = new DateRangeFilter(
        LocalDate.of(2025,6,1),
        LocalDate.of(2025,6,30)
    );
    Budget b1 = new Budget(); b1.setId(1L); b1.setAmount(new BigDecimal("100"));
    Budget b2 = new Budget(); b2.setId(2L); b2.setAmount(new BigDecimal("200"));
    when(budgetService.getAllBudgets(any(DateRangeFilter.class))).thenReturn(List.of(b1, b2));

    mockMvc.perform(get("/api/budgets")
            .param("start", "2025-06-01")
            .param("end",   "2025-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].amount").value(200));
}

@Test
void createBudget_returnsCreated() throws Exception {
    BudgetCreationRequest req = new BudgetCreationRequest(
        new BigDecimal("150"),
        LocalDate.of(2025,7,1),
        LocalDate.of(2025,7,31),
        5L  
    );
    Budget saved = new Budget(); saved.setId(7L); saved.setAmount(req.getAmount());
    when(budgetService.saveBudget(any(BudgetCreationRequest.class))).thenReturn(saved);

    mockMvc.perform(post("/api/budgets")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/budgets/7")))
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.amount").value(150));
}

@Test
void deleteBudget_returnsNoContent() throws Exception {
    // no stub needed for void service
    mockMvc.perform(delete("/api/budgets/13"))
        .andExpect(status().isNoContent());

    verify(budgetService).deleteBudgetForUser(13L);
}

@Test
void updateBudgetAmount_returnsUpdated() throws Exception {
    UpdateBudgetAmountDto dto = new UpdateBudgetAmountDto(new BigDecimal("250"));
    Budget updated = new Budget(); updated.setId(5L); updated.setAmount(dto.getAmount());
    when(budgetService.updateBudgetAmount(eq(5L), any(UpdateBudgetAmountDto.class))).thenReturn(updated);

    mockMvc.perform(patch("/api/budgets/5")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.amount").value(250));
}

@Test
void getTotalSpentOnBudget_returnsAmount() throws Exception {
    when(budgetService.getTotalSpentOnBudget(11L)).thenReturn(new BigDecimal("75.50"));

    mockMvc.perform(get("/api/budgets/spent/11"))
        .andExpect(status().isOk())
        .andExpect(content().string("75.50"));
}

@Test
void updateMonthlyStatus_returnsBudget() throws Exception {
    Budget toggled = new Budget(); toggled.setId(9L); toggled.setMonthly(true);
    when(budgetService.updateMonthlyStatus(9L, true)).thenReturn(toggled);

    mockMvc.perform(patch("/api/budgets/9/monthly")
            .param("active", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.monthly").value(true));
}

    
}
