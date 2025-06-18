package JK.pfm.controller;  // <-- match your real package!

import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Transaction;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.TransactionService;
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


@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean TransactionService transactionService;

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

    // POST /api/transactions
@Test
void createTransaction_returnsCreated() throws Exception {
    TransactionCreationRequest req = new TransactionCreationRequest(
        LocalDate.of(2025, 6, 15),
        new BigDecimal("125.00"),
        3L,
        "Checking",
        "Expense",
        "Test txn"
    );
    Transaction saved = new Transaction();
    saved.setId(42L);
    when(transactionService.saveTransaction(any(TransactionCreationRequest.class)))
        .thenReturn(saved);

    mockMvc.perform(post("/api/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/transactions/42")))
        .andExpect(jsonPath("$.id").value(42));

    verify(transactionService).saveTransaction(any(TransactionCreationRequest.class));
}

// GET /api/transactions/{id} – found
@Test
void getTransaction_returnsOkWhenFound() throws Exception {
    Transaction txn = new Transaction();
    txn.setId(7L);
    when(transactionService.getTransactionById(7L))
        .thenReturn(Optional.of(txn));

    mockMvc.perform(get("/api/transactions/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7));

    verify(transactionService).getTransactionById(7L);
}

// GET /api/transactions/{id} – not found
@Test
void getTransaction_returnsNotFoundWhenMissing() throws Exception {
    when(transactionService.getTransactionById(99L))
        .thenReturn(Optional.empty());

    mockMvc.perform(get("/api/transactions/99"))
        .andExpect(status().isNotFound());

    verify(transactionService).getTransactionById(99L);
}

// DELETE /api/transactions/{id}
@Test
void deleteTransaction_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/transactions/15"))
        .andExpect(status().isNoContent());

    verify(transactionService).deleteTransaction(15L);
}

// GET /api/transactions with filters
@Test
void getTransactions_returnsFilteredList() throws Exception {
    // simulate authenticated user
    CustomUserDetails principal = new CustomUserDetails(55L, "user", "pw");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null)
    );

    Transaction t1 = new Transaction(); t1.setId(1L);
    Transaction t2 = new Transaction(); t2.setId(2L);
    when(transactionService.getTransactionsByFilters(
            eq(LocalDate.of(2025, 6, 1)),
            eq(LocalDate.of(2025, 6, 30)),
            eq(3L),
            eq(4L),
            eq(55L),
            eq("Expense")
        )).thenReturn(List.of(t1, t2));

    mockMvc.perform(get("/api/transactions")
            .param("startDate", "2025-06-01")
            .param("endDate",   "2025-06-30")
            .param("categoryId","3")
            .param("accountId", "4")
            .param("type",      "Expense"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(2));

    verify(transactionService).getTransactionsByFilters(
        LocalDate.of(2025, 6, 1),
        LocalDate.of(2025, 6, 30),
        3L, 4L, 55L, "Expense"
    );
}

// GET /api/transactions/recent
@Test
void getRecentTransactions_returnsList() throws Exception {
    Transaction r1 = new Transaction(); r1.setId(10L);
    Transaction r2 = new Transaction(); r2.setId(11L);
    when(transactionService.getRecentTransactions())
        .thenReturn(List.of(r1, r2));

    mockMvc.perform(get("/api/transactions/recent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[1].id").value(11));

    verify(transactionService).getRecentTransactions();
}

   
}
