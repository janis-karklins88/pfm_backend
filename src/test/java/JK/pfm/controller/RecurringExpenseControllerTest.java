package JK.pfm.controller;  // <-- match your real package!

import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.UpdatePaymentAmountDto;
import JK.pfm.dto.UpdatePaymentNextDueDateDto;
import JK.pfm.dto.UpdateRecurringExpenseAccountDto;
import JK.pfm.dto.filters.ReccurringExpenseFilter;
import JK.pfm.model.Account;
import JK.pfm.model.RecurringExpense;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.RecurringExpenseService;
import JK.pfm.util.JWTUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;



import java.time.LocalDate;
import java.util.List;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = RecurringExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecurringExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean
    private RecurringExpenseService recurringExpenseService;

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

    // GET /api/recurring-expenses
@Test
void getRecurringExpenses_returnsList() throws Exception {
    RecurringExpense r1 = new RecurringExpense(); r1.setId(1L);
    RecurringExpense r2 = new RecurringExpense(); r2.setId(2L);
    when(recurringExpenseService.getRecurringExpensesByFilters(any(ReccurringExpenseFilter.class)))
        .thenReturn(List.of(r1, r2));

    mockMvc.perform(get("/api/recurring-expenses")
            .param("start", "2025-06-01")
            .param("end",   "2025-06-30")
            .param("categoryId", "3")
            .param("accountId",  "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(2));

    verify(recurringExpenseService)
        .getRecurringExpensesByFilters(any(ReccurringExpenseFilter.class));
}

// POST /api/recurring-expenses
@Test
void createRecurringExpense_returnsCreated() throws Exception {
    RecurringExpenseCreation req = new RecurringExpenseCreation(
        "test-payment", LocalDate.of(2025,7,1), new BigDecimal("20"), 1L, "test-acc", "MONTHLY"
    );
    RecurringExpense saved = new RecurringExpense(); saved.setId(10L);
    when(recurringExpenseService.saveRecurringExpense(any(RecurringExpenseCreation.class)))
        .thenReturn(saved);

    mockMvc.perform(post("/api/recurring-expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/recurring-expenses/10")))
        .andExpect(jsonPath("$.id").value(10));

    verify(recurringExpenseService).saveRecurringExpense(any(RecurringExpenseCreation.class));
}

// GET /api/recurring-expenses/next-payments
@Test
void getNextPayments_returnsList() throws Exception {
    RecurringExpense next1 = new RecurringExpense(); next1.setId(5L);
    RecurringExpense next2 = new RecurringExpense(); next2.setId(6L);
    when(recurringExpenseService.getUpcommingRecurringExpense())
        .thenReturn(List.of(next1, next2));

    mockMvc.perform(get("/api/recurring-expenses/next-payments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(5))
        .andExpect(jsonPath("$[1].id").value(6));

    verify(recurringExpenseService).getUpcommingRecurringExpense();
}

// DELETE /api/recurring-expenses/{id}
@Test
void deleteRecurringExpense_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/recurring-expenses/7"))
        .andExpect(status().isNoContent());

    verify(recurringExpenseService).deleteRecurringExpense(7L);
}

// PATCH /api/recurring-expenses/amount/{id}
@Test
void updateRecurringExpenseAmount_returnsUpdated() throws Exception {
    UpdatePaymentAmountDto dto = new UpdatePaymentAmountDto(new BigDecimal("50"));
    RecurringExpense updated = new RecurringExpense(); updated.setId(7L); updated.setAmount(new BigDecimal("50"));
    when(recurringExpenseService.updateRecurringExpenseAmount(
            eq(7L), any(UpdatePaymentAmountDto.class)))
        .thenReturn(updated);

    mockMvc.perform(patch("/api/recurring-expenses/amount/7")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.amount").value(50));

    verify(recurringExpenseService)
        .updateRecurringExpenseAmount(eq(7L), any(UpdatePaymentAmountDto.class));
}

// PATCH /api/recurring-expenses/name/{id}
@Test
void updateRecurringExpenseNextDueDate_returnsUpdated() throws Exception {
    UpdatePaymentNextDueDateDto dto = new UpdatePaymentNextDueDateDto(LocalDate.of(2025,8,1));
    RecurringExpense updated = new RecurringExpense(); updated.setId(8L); updated.setNextDueDate(LocalDate.of(2025,8,1));
    when(recurringExpenseService.updateRecurringExpenseNextDueDate(
            eq(8L), any(UpdatePaymentNextDueDateDto.class)))
        .thenReturn(updated);

    mockMvc.perform(patch("/api/recurring-expenses/name/8")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(8))
        .andExpect(jsonPath("$.nextDueDate").value("2025-08-01"));

    verify(recurringExpenseService)
        .updateRecurringExpenseNextDueDate(eq(8L), any(UpdatePaymentNextDueDateDto.class));
}

// PATCH /api/recurring-expenses/account/{id}
@Test
void updateRecurringExpenseAccount_returnsUpdated() throws Exception {
    UpdateRecurringExpenseAccountDto dto = new UpdateRecurringExpenseAccountDto(4L);
    RecurringExpense updated = new RecurringExpense(); 
    updated.setId(9L);
    updated.setAccount(new Account());
    updated.getAccount().setId(4L);
    when(recurringExpenseService.updateRecurringExpenseAccount(
            eq(9L), any(UpdateRecurringExpenseAccountDto.class)))
        .thenReturn(updated);

    mockMvc.perform(patch("/api/recurring-expenses/account/9")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.account.id").value(4));

    verify(recurringExpenseService)
        .updateRecurringExpenseAccount(eq(9L), any(UpdateRecurringExpenseAccountDto.class));
}

// PATCH /api/recurring-expenses/{id}/pause
@Test
void pauseRecurringExpense_returnsPaused() throws Exception {
    RecurringExpense paused = new RecurringExpense(); paused.setId(11L); paused.setActive(false);
    when(recurringExpenseService.pauseRecurringExpense(11L)).thenReturn(paused);

    mockMvc.perform(patch("/api/recurring-expenses/11/pause"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(11))
        .andExpect(jsonPath("$.active").value(false));

    verify(recurringExpenseService).pauseRecurringExpense(11L);
}

// PATCH /api/recurring-expenses/{id}/resume
@Test
void resumeRecurringExpense_returnsResumed() throws Exception {
    RecurringExpense resumed = new RecurringExpense(); resumed.setId(12L); resumed.setActive(true);
    when(recurringExpenseService.resumeRecurringExpense(12L)).thenReturn(resumed);

    mockMvc.perform(patch("/api/recurring-expenses/12/resume"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(12))
        .andExpect(jsonPath("$.active").value(true));

    verify(recurringExpenseService).resumeRecurringExpense(12L);
}

    
}
