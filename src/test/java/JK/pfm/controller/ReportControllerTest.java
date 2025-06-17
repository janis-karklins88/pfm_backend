package JK.pfm.controller;  // <-- match your real package!

import JK.pfm.dto.BalanceBreakdownDTO;
import JK.pfm.dto.CashFlowDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import JK.pfm.dto.ChangesVsLastMonthDTO;
import JK.pfm.dto.ExpenseByAccountDTO;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.dto.filters.DateRangeFilter;
import JK.pfm.security.CustomUserDetailsService;
import JK.pfm.service.ReportService;
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



import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.hasSize;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mock your service
    @MockitoBean ReportService reportService;

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

    // GET /api/reports/summary
@Test
void getSpendingAndIncomeSummary_returnsMap() throws Exception {
    Map<String, BigDecimal> summary = Map.of(
        "income",  new BigDecimal("100.00"),
        "expenses", new BigDecimal("40.00")
    );
    when(reportService.getSpendingAndIncomeSummary(any(DateRangeFilter.class)))
        .thenReturn(summary);

    mockMvc.perform(get("/api/reports/summary")
            .param("start", "2025-06-01")
            .param("end",   "2025-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.income").value(100.00))
        .andExpect(jsonPath("$.expenses").value(40.00));

    verify(reportService).getSpendingAndIncomeSummary(any(DateRangeFilter.class));
}

// GET /api/reports/spending-by-category
@Test
void getSpendingByCategory_returnsList() throws Exception {
    ExpenseByCategoryDTO e1 = new ExpenseByCategoryDTO("Food", new BigDecimal("25.00"));
    ExpenseByCategoryDTO e2 = new ExpenseByCategoryDTO("Rent", new BigDecimal("75.00"));
    when(reportService.getSpendingByCategory(any(DateRangeFilter.class)))
        .thenReturn(List.of(e1, e2));

    mockMvc.perform(get("/api/reports/spending-by-category")
            .param("start", "2025-06-01")
            .param("end",   "2025-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].categoryName").value("Food"))
        .andExpect(jsonPath("$[1].totalAmount").value(75.00));

    verify(reportService).getSpendingByCategory(any(DateRangeFilter.class));
}

// GET /api/reports/spending-by-account
@Test
void getSpendingByAccount_returnsList() throws Exception {
    ExpenseByAccountDTO a1 = new ExpenseByAccountDTO("Checking", new BigDecimal("60.00"));
    ExpenseByAccountDTO a2 = new ExpenseByAccountDTO("Savings",  new BigDecimal("40.00"));
    when(reportService.getSpendingByAccount(any(DateRangeFilter.class)))
        .thenReturn(List.of(a1, a2));

    mockMvc.perform(get("/api/reports/spending-by-account")
            .param("start", "2025-06-01")
            .param("end",   "2025-06-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].accountName").value("Checking"))
        .andExpect(jsonPath("$[1].totalAmount").value(40.00));

    verify(reportService).getSpendingByAccount(any(DateRangeFilter.class));
}

// GET /api/reports/balance
@Test
void getTotalUserBalance_returnsBalance() throws Exception {
    when(reportService.getTotalUserBalance()).thenReturn(new BigDecimal("350.75"));

    mockMvc.perform(get("/api/reports/balance"))
        .andExpect(status().isOk())
        .andExpect(content().string("350.75"));

    verify(reportService).getTotalUserBalance();
}

// GET /api/reports/monthly-cashflow
@Test
void getMonthlyCashFlow_returnsList() throws Exception {
    CashFlowDTO cf1 = new CashFlowDTO("jan",
        new BigDecimal("10.00"),
        new BigDecimal("50.00"),
        new BigDecimal("-40.00")
    );
    CashFlowDTO cf2 = new CashFlowDTO("feb",
        new BigDecimal("50.00"),
        new BigDecimal("30.00"),
        new BigDecimal("20.00")
    );
    when(reportService.getMonthlyCashFlow())
        .thenReturn(List.of(cf1, cf2));

    mockMvc.perform(get("/api/reports/monthly-cashflow"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        // first element
        .andExpect(jsonPath("$[0].month").value("jan"))
        .andExpect(jsonPath("$[0].inflow").value(10.00))
        .andExpect(jsonPath("$[0].outflow").value(50.00))
        .andExpect(jsonPath("$[0].netFlow").value(-40.00))
        // second element
        .andExpect(jsonPath("$[1].month").value("feb"))
        .andExpect(jsonPath("$[1].inflow").value(50.00))
        .andExpect(jsonPath("$[1].outflow").value(30.00))
        .andExpect(jsonPath("$[1].netFlow").value(20.00));

    verify(reportService).getMonthlyCashFlow();
}


// GET /api/reports/balance-breakdown
@Test
void getBalanceBreakdown_returnsList() throws Exception {
    BalanceBreakdownDTO b1 = new BalanceBreakdownDTO("Checking", new BigDecimal("200.00"));
    BalanceBreakdownDTO b2 = new BalanceBreakdownDTO("Savings",  new BigDecimal("150.75"));
    when(reportService.getBalanceBreakdown())
        .thenReturn(List.of(b1, b2));

    mockMvc.perform(get("/api/reports/balance-breakdown"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value("Checking"))
        .andExpect(jsonPath("$[1].amount").value(150.75));

    verify(reportService).getBalanceBreakdown();
}

// GET /api/reports/expense-and-prediction
@Test
void getExpenseAndPrediction_returnsMap() throws Exception {
    Map<String, BigDecimal> map = Map.of(
        "2025-05", new BigDecimal("100.00"),
        "2025-06", new BigDecimal("120.00")
    );
    when(reportService.getExpenseAndPrediction()).thenReturn(map);

    mockMvc.perform(get("/api/reports/expense-and-prediction"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.['2025-05']").value(100.00))
        .andExpect(jsonPath("$.['2025-06']").value(120.00));

    verify(reportService).getExpenseAndPrediction();
}

// GET /api/reports/expense-for-category
@Test
void getExpenseByCategory_returnsMap() throws Exception {
    Map<String, BigDecimal> map = Map.of("2025-06", new BigDecimal("45.00"));
    when(reportService.getExpenseForCategory(3L)).thenReturn(map);

    mockMvc.perform(get("/api/reports/expense-for-category")
            .param("categoryId", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.['2025-06']").value(45.00));

    verify(reportService).getExpenseForCategory(3L);
}

// GET /api/reports/balance-change
@Test
void getAmountChanges_returnsList() throws Exception {
    ChangesVsLastMonthDTO c1 = new ChangesVsLastMonthDTO("Food", new BigDecimal("10.00"));
    ChangesVsLastMonthDTO c2 = new ChangesVsLastMonthDTO("Rent", new BigDecimal("-5.00"));
    when(reportService.getChanges()).thenReturn(List.of(c1, c2));

    mockMvc.perform(get("/api/reports/balance-change"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value("Food"))
        .andExpect(jsonPath("$[1].percentage").value(-5.00));

    verify(reportService).getChanges();
}

}
