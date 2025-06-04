
package JK.pfm;

import JK.pfm.dto.CashFlowDTO;
import JK.pfm.dto.ChangesVsLastMonthDTO;
import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.UpdatePaymentAmountDto;
import JK.pfm.dto.UpdatePaymentNextDueDateDto;
import JK.pfm.dto.UpdateRecurringExpenseAccountDto;
import JK.pfm.dto.filters.DateRangeFilter;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.AccountService;
import JK.pfm.service.RecurringExpenseService;
import JK.pfm.service.ReportService;
import JK.pfm.service.SavingsGoalService;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReportServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountService accountService;
    @Mock SavingsGoalService savingsGoalsService;
    @Mock AccountRepository accountRepository;
    @Mock SavingsGoalService savingsGoalService;
    @Mock SavingsGoalRepository savingsGoalRepository;
    @Mock AccountUtil accountUtil;
    
    @InjectMocks
    ReportService reportService;

    private User user;
    private MockedStatic<SecurityUtil> securityUtilMock;
    
    @BeforeEach
    void setUp(){
        securityUtilMock = mockStatic(SecurityUtil.class);
        
        user = new User();
        user.setId(1L);
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(1L);

        
    }
    
    @AfterEach
    void tearDown(){
        securityUtilMock.close();
    }
    
    @Test
    void getSpendingAndIncomeSummary_noDates_usesDefaultRange() {
    // Arrange: user has two accounts
    List<Long> accountIds = List.of(1L, 2L);
    when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

    // Stub sums for default range
    BigDecimal expectedSpending = new BigDecimal("250.00");
    BigDecimal expectedIncome   = new BigDecimal("500.00");

    // Default start = 1900-01-01, default end = today
    LocalDate defaultStart = LocalDate.of(1900, 1, 1);
    LocalDate defaultEnd   = LocalDate.now();

    when(transactionRepository.sumByTypeAndDate("Expense", accountIds, defaultStart, defaultEnd))
        .thenReturn(expectedSpending);
    when(transactionRepository.sumByTypeAndDate("Deposit", accountIds, defaultStart, defaultEnd))
        .thenReturn(expectedIncome);

    DateRangeFilter filter = new DateRangeFilter(null, null);

    // Act
    Map<String, BigDecimal> result = reportService.getSpendingAndIncomeSummary(filter);

    // Assert: map contains both values
    assertThat(result).hasSize(2);
    assertThat(result.get("totalSpending")).isEqualByComparingTo(expectedSpending);
    assertThat(result.get("totalIncome")).isEqualByComparingTo(expectedIncome);

    // Verify repository calls used the default dates
    verify(transactionRepository).sumByTypeAndDate("Expense", accountIds, defaultStart, defaultEnd);
    verify(transactionRepository).sumByTypeAndDate("Deposit", accountIds, defaultStart, defaultEnd);
    }

    @Test
    void getSpendingAndIncomeSummary_withDates_usesProvidedRange() {
    // Arrange: user has one account
    List<Long> accountIds = List.of(42L);
    when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

    LocalDate start = LocalDate.of(2025, 1, 1);
    LocalDate end   = LocalDate.of(2025, 1, 31);

    BigDecimal expectedSpending = new BigDecimal("120.00");
    BigDecimal expectedIncome   = new BigDecimal("300.00");

    when(transactionRepository.sumByTypeAndDate("Expense", accountIds, start, end))
        .thenReturn(expectedSpending);
    when(transactionRepository.sumByTypeAndDate("Deposit", accountIds, start, end))
        .thenReturn(expectedIncome);

    DateRangeFilter filter = new DateRangeFilter(start, end);

    // Act
    Map<String, BigDecimal> result = reportService.getSpendingAndIncomeSummary(filter);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result.get("totalSpending")).isEqualByComparingTo(expectedSpending);
    assertThat(result.get("totalIncome")).isEqualByComparingTo(expectedIncome);

    // Verify repository calls used the provided dates
    verify(transactionRepository).sumByTypeAndDate("Expense", accountIds, start, end);
    verify(transactionRepository).sumByTypeAndDate("Deposit", accountIds, start, end);
    }
    
    @Test
    void getMonthlyCashFlow_allZeroSummary_returnsSevenZeroEntries() {
    // Arrange
    // Create a spy of the ReportService so we can stub getSpendingAndIncomeSummary(...)
    ReportService serviceSpy = Mockito.spy(reportService);

    // Stub getSpendingAndIncomeSummary to always return an empty map (zeros)
    doReturn(Collections.emptyMap())
    .when(serviceSpy)
    .getSpendingAndIncomeSummary(any(DateRangeFilter.class));
    
    // Act
    List<CashFlowDTO> result = serviceSpy.getMonthlyCashFlow();

    // Assert: we should have 7 entries (months i = 6 down to 0)
    assertThat(result).hasSize(7);

    LocalDate today = LocalDate.now();
    for (int i = 6; i >= 0; i--) {
        int index = 6 - i; // result[0] corresponds to targetMonth = today.minusMonths(6)
        CashFlowDTO dto = result.get(index);
        String expectedMonthLabel = today.minusMonths(i).getMonth().toString();

        assertThat(dto.getMonth()).isEqualTo(expectedMonthLabel);
        assertThat(dto.getInflow()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getOutflow()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getNetFlow()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Verify that getSpendingAndIncomeSummary(...) was called exactly 7 times
    verify(serviceSpy, times(7)).getSpendingAndIncomeSummary(any(DateRangeFilter.class));
    }

    @Test
    void getMonthlyCashFlow_nonZeroSummary_appliesValuesCorrectly() {
    // Arrange
    ReportService serviceSpy = Mockito.spy(reportService);

    // Prepare a summary map with fixed values
    Map<String, BigDecimal> sampleSummary = new HashMap<>();
    sampleSummary.put("totalIncome", new BigDecimal("200"));
    sampleSummary.put("totalSpending", new BigDecimal("150"));

    // Stub getSpendingAndIncomeSummary(...) to always return sampleSummary
    doReturn(sampleSummary)
    .when(serviceSpy)
    .getSpendingAndIncomeSummary(any(DateRangeFilter.class));

    // Act
    List<CashFlowDTO> result = serviceSpy.getMonthlyCashFlow();

    // Assert
    assertThat(result).hasSize(7);

    LocalDate today = LocalDate.now();
    for (int i = 6; i >= 0; i--) {
        int index = 6 - i;
        CashFlowDTO dto = result.get(index);
        String expectedMonthLabel = today.minusMonths(i).getMonth().toString();

        assertThat(dto.getMonth()).isEqualTo(expectedMonthLabel);
        assertThat(dto.getInflow()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(dto.getOutflow()).isEqualByComparingTo(new BigDecimal("150"));
        // netFlow = 200 - 150 = 50
        assertThat(dto.getNetFlow()).isEqualByComparingTo(new BigDecimal("50"));
    }

    // Verify it was called 7 times
    verify(serviceSpy, times(7)).getSpendingAndIncomeSummary(any(DateRangeFilter.class));
    }
    
    @Test
    void getExpenseAndPrediction_allZeroHistoricMonths_predictionsZero() {
    // Arrange
    List<Long> accountIds = List.of(1L, 2L);
    when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

    LocalDate today = LocalDate.now();

    // By default, every historic month returns BigDecimal.ZERO
    when(transactionRepository.sumByTypeAndDate(
            eq("Expense"),
            eq(accountIds),
            any(LocalDate.class),
            any(LocalDate.class)))
        .thenReturn(BigDecimal.ZERO);

    // Act
    Map<String, BigDecimal> result = reportService.getExpenseAndPrediction();

    // Assert: should have 10 historical month labels + 2 future months = 12 entries
    assertThat(result).hasSize(12);

    // 1) Every historic month should map to 0
    for (int i = 9; i >= 0; i--) {
        LocalDate targetMonth = today.minusMonths(i);
        String monthLabel = targetMonth.withDayOfMonth(1)
                                       .getMonth()
                                       .toString()
                                       .substring(0, 3);
        assertThat(result.get(monthLabel)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // 2) Predictions for next two months should also be zero
    String nextLabel     = today.plusMonths(1).getMonth().toString().substring(0, 3);
    String nextNextLabel = today.plusMonths(2).getMonth().toString().substring(0, 3);
    assertThat(result.get(nextLabel)).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.get(nextNextLabel)).isEqualByComparingTo(BigDecimal.ZERO);

    // Verify that sumByTypeAndDate was called 10 times (once per historic month)
    for (int i = 9; i >= 0; i--) {
        LocalDate target = today.minusMonths(i);
        LocalDate start = target.withDayOfMonth(1);
        LocalDate end   = target.withDayOfMonth(target.lengthOfMonth());
        verify(transactionRepository).sumByTypeAndDate("Expense", accountIds, start, end);
    }
}

    @Test
    void getExpenseAndPrediction_partialHistoricData_computesCorrectPrediction() {
    // Arrange
    List<Long> accountIds = List.of(1L);
    when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

    LocalDate today = LocalDate.now();

    // Default: every month = 0
    when(transactionRepository.sumByTypeAndDate(
            eq("Expense"),
            eq(accountIds),
            any(LocalDate.class),
            any(LocalDate.class)))
        .thenReturn(BigDecimal.ZERO);

    // Provide actual expenses for two months: i=9 and i=8
    LocalDate month9 = today.minusMonths(9);
    LocalDate start9 = month9.withDayOfMonth(1);
    LocalDate end9   = month9.withDayOfMonth(month9.lengthOfMonth());
    when(transactionRepository.sumByTypeAndDate("Expense", accountIds, start9, end9))
        .thenReturn(new BigDecimal("100"));

    LocalDate month8 = today.minusMonths(8);
    LocalDate start8 = month8.withDayOfMonth(1);
    LocalDate end8   = month8.withDayOfMonth(month8.lengthOfMonth());
    when(transactionRepository.sumByTypeAndDate("Expense", accountIds, start8, end8))
        .thenReturn(new BigDecimal("50"));

    // Act
    Map<String, BigDecimal> result = reportService.getExpenseAndPrediction();

    // Assert: 10 historical months + 2 predictions = 12 entries
    assertThat(result).hasSize(12);

    // 1) Check the two non-zero months
    String label9 = month9.getMonth().toString().substring(0, 3);
    String label8 = month8.getMonth().toString().substring(0, 3);
    assertThat(result.get(label9)).isEqualByComparingTo("100");
    assertThat(result.get(label8)).isEqualByComparingTo("50");

    // All other historical months (i=7..0) should map to 0
    for (int i = 7; i >= 0; i--) {
        LocalDate m = today.minusMonths(i);
        String lbl = m.getMonth().toString().substring(0, 3);
        assertThat(result.get(lbl)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // 2) Compute expected prediction:
    //   sumExpenses = 100 + 50 = 150
    //   monthsWithData = 2 (since only those two months > 0)
    //   totalAverage = 150 / 2 = 75.00
    //   recentExpenses (i>0 && i<=3) = none of the non-zero months fall in i=1..3, so = 0
    //   recentAverage = 0 / 3 = 0.00
    //   totalAverage != 0, but monthsWithData < 6 ⇒ predictedExpense = totalAverage = 75.00
    BigDecimal expectedPrediction = new BigDecimal("75.00");

    String nextLabel     = today.plusMonths(1).getMonth().toString().substring(0, 3);
    String nextNextLabel = today.plusMonths(2).getMonth().toString().substring(0, 3);
    assertThat(result.get(nextLabel)).isEqualByComparingTo(expectedPrediction);
    assertThat(result.get(nextNextLabel)).isEqualByComparingTo(expectedPrediction);

    // Verify that sumByTypeAndDate was called for each of the 10 historical months
    for (int i = 9; i >= 0; i--) {
        LocalDate m     = today.minusMonths(i);
        LocalDate start = m.withDayOfMonth(1);
        LocalDate end   = m.withDayOfMonth(m.lengthOfMonth());
        verify(transactionRepository).sumByTypeAndDate("Expense", accountIds, start, end);
    }
    }
    
    @Test
    void getExpenseForCategory_allZeroHistoricMonths_predictionsZero() {
    // Arrange
    List<Long> accountIds = List.of(1L, 2L);
    when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

    LocalDate today = LocalDate.now();

    // By default, sumByTypeAndDateAndCategory(...) → BigDecimal.ZERO, existsBy... → false
    when(transactionRepository.sumByTypeAndDateAndCategory(
            eq("Expense"), eq(accountIds), any(LocalDate.class), any(LocalDate.class), eq(5L)))
        .thenReturn(BigDecimal.ZERO);
    when(transactionRepository.existsByAccountIdInAndDateBetween(
            eq(accountIds), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(false);

    // Act
    Map<String, BigDecimal> result = reportService.getExpenseForCategory(5L);

    // Assert: should have 10 historical months + 2 future months = 12 entries
    assertThat(result).hasSize(12);

    // 1) Every historical month label → 0
    for (int i = 9; i >= 0; i--) {
        LocalDate target = today.minusMonths(i);
        String monthLabel = target.withDayOfMonth(1)
                                  .getMonth()
                                  .toString()
                                  .substring(0, 3);
        assertThat(result.get(monthLabel)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // 2) Predictions for next two months also → 0
    String nextLabel     = today.plusMonths(1).getMonth().toString().substring(0, 3);
    String nextNextLabel = today.plusMonths(2).getMonth().toString().substring(0, 3);
    assertThat(result.get(nextLabel)).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.get(nextNextLabel)).isEqualByComparingTo(BigDecimal.ZERO);

    // Verify repository calls for each of the 10 months
    for (int i = 9; i >= 0; i--) {
        LocalDate m = today.minusMonths(i);
        LocalDate start = m.withDayOfMonth(1);
        LocalDate end   = m.withDayOfMonth(m.lengthOfMonth());
        verify(transactionRepository)
            .sumByTypeAndDateAndCategory("Expense", accountIds, start, end, 5L);
        verify(transactionRepository)
            .existsByAccountIdInAndDateBetween(accountIds, start, end);
    }
}

    @Test
    void getExpenseForCategory_partialHistoricData_computesCorrectPrediction() {
    // Arrange
    List<Long> accountIds = List.of(42L);
    when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

    LocalDate today = LocalDate.now();

    // Default stubs: sum → 0, exists → false
    when(transactionRepository.sumByTypeAndDateAndCategory(
            eq("Expense"), eq(accountIds), any(LocalDate.class), any(LocalDate.class), eq(7L)))
        .thenReturn(BigDecimal.ZERO);
    when(transactionRepository.existsByAccountIdInAndDateBetween(
            eq(accountIds), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(false);

    // Provide actual sums and existence for two months: i=9 and i=5
    LocalDate m9 = today.minusMonths(9);
    LocalDate s9 = m9.withDayOfMonth(1);
    LocalDate e9 = m9.withDayOfMonth(m9.lengthOfMonth());
    when(transactionRepository.sumByTypeAndDateAndCategory("Expense", accountIds, s9, e9, 7L))
        .thenReturn(new BigDecimal("100"));
    when(transactionRepository.existsByAccountIdInAndDateBetween(accountIds, s9, e9))
        .thenReturn(true);

    LocalDate m5 = today.minusMonths(5);
    LocalDate s5 = m5.withDayOfMonth(1);
    LocalDate e5 = m5.withDayOfMonth(m5.lengthOfMonth());
    when(transactionRepository.sumByTypeAndDateAndCategory("Expense", accountIds, s5, e5, 7L))
        .thenReturn(new BigDecimal("50"));
    when(transactionRepository.existsByAccountIdInAndDateBetween(accountIds, s5, e5))
        .thenReturn(true);

    // Act
    Map<String, BigDecimal> result = reportService.getExpenseForCategory(7L);

    // Assert: 12 entries total
    assertThat(result).hasSize(12);

    // 1) Check the two non-zero historical months
    String label9 = m9.getMonth().toString().substring(0, 3);
    String label5 = m5.getMonth().toString().substring(0, 3);
    assertThat(result.get(label9)).isEqualByComparingTo("100");
    assertThat(result.get(label5)).isEqualByComparingTo("50");

    // Other historical months → 0
    for (int i = 9; i >= 0; i--) {
        if (i == 9 || i == 5) continue;
        LocalDate m = today.minusMonths(i);
        String lbl = m.getMonth().toString().substring(0, 3);
        assertThat(result.get(lbl)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // 2) Compute expected prediction:
    //   sumExpenses = 100 + 50 = 150
    //   monthsWithData = 2
    //   totalAverage = 150 / 2 = 75.00
    //   recentExpenses: only months with i>0 && i<=3. Our non-zeros are at i=9 and i=5, so recentExpenses=0
    //   trendFactor not used since monthsWithData<6
    BigDecimal expectedPrediction = new BigDecimal("75.00");

    String nextLabel     = today.plusMonths(1).getMonth().toString().substring(0, 3);
    String nextNextLabel = today.plusMonths(2).getMonth().toString().substring(0, 3);
    assertThat(result.get(nextLabel)).isEqualByComparingTo(expectedPrediction);
    assertThat(result.get(nextNextLabel)).isEqualByComparingTo(expectedPrediction);

    // Verify repository calls for all 10 historical months
    for (int i = 9; i >= 0; i--) {
        LocalDate m = today.minusMonths(i);
        LocalDate start = m.withDayOfMonth(1);
        LocalDate end   = m.withDayOfMonth(m.lengthOfMonth());
        verify(transactionRepository)
            .sumByTypeAndDateAndCategory("Expense", accountIds, start, end, 7L);
        verify(transactionRepository)
            .existsByAccountIdInAndDateBetween(accountIds, start, end);
    }
    }
    
    @Test
    void getChanges_nonZeroValues_computesCorrectPercentages() {
        // Arrange
        ReportService serviceSpy = Mockito.spy(reportService);

        List<Long> accountIds = List.of(10L, 20L);
        when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);

        LocalDate startThis = today.withDayOfMonth(1);
        LocalDate endThis   = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate startLast = lastMonth.withDayOfMonth(1);
        LocalDate endLast   = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());

        // Build maps for this month and last month:
        Map<String, BigDecimal> thisSummary = new HashMap<>();
        thisSummary.put("totalIncome", new BigDecimal("200"));
        thisSummary.put("totalSpending", new BigDecimal("80"));

        Map<String, BigDecimal> lastSummary = new HashMap<>();
        lastSummary.put("totalIncome", new BigDecimal("100"));
        lastSummary.put("totalSpending", new BigDecimal("100"));

        // Stub getSpendingAndIncomeSummary(...) in sequence: first call → thisSummary, second → lastSummary
        doReturn(thisSummary)
            .doReturn(lastSummary)
            .when(serviceSpy)
            .getSpendingAndIncomeSummary(any(DateRangeFilter.class));

        // Stub savingsGoalRepository balances:
        // currentSav = 300, lastSav = 150
        when(savingsGoalRepository.getTotalBalanceByUserId(1L))
            .thenReturn(new BigDecimal("300"));
        when(savingsGoalRepository.getLastMonthBalanceByUserId(1L))
            .thenReturn(new BigDecimal("150"));

        // Stub getTotalUserBalance() → 1000
        doReturn(new BigDecimal("1000")).when(serviceSpy).getTotalUserBalance();

        // Stub transactionRepository.getAccountBalanceUpTo(userId, endLast) → 700
        when(transactionRepository.getAccountBalanceUpTo(1L, endLast))
            .thenReturn(new BigDecimal("700"));

        // Act
        List<ChangesVsLastMonthDTO> changes = serviceSpy.getChanges();

        // Assert: five entries in order:
        // 1. Income: percent = (200 - 100) *100 /100 = 100
        // 2. Expense: percent = (80 - 100) *100 /100 = −20
        // 3. Savings: percent = (300 - 150) *100 /150 = 100
        // 4. totalBalance: lastTotal = lastAccounts(700) + lastSav(150) = 850
        //    percent = (1000 - 850)*100 /850 ≈ 18 (floor from 17.65… rounds to 18)
        // 5. accountBalance: percent = (currentTotal(1000) - lastAccounts(700))*100 /700 ≈ 43 (300*100/700=42.857… rounds to 43)
        assertThat(changes).hasSize(5);

        ChangesVsLastMonthDTO incomeDto   = changes.get(0);
        ChangesVsLastMonthDTO expenseDto  = changes.get(1);
        ChangesVsLastMonthDTO savingsDto  = changes.get(2);
        ChangesVsLastMonthDTO totalDto    = changes.get(3);
        ChangesVsLastMonthDTO accountDto  = changes.get(4);

        assertThat(incomeDto.getName()).isEqualTo("Income");
        assertThat(incomeDto.getPercentage()).isEqualByComparingTo(new BigDecimal("100"));

        assertThat(expenseDto.getName()).isEqualTo("Expense");
        assertThat(expenseDto.getPercentage()).isEqualByComparingTo(new BigDecimal("-20"));

        assertThat(savingsDto.getName()).isEqualTo("Savings");
        assertThat(savingsDto.getPercentage()).isEqualByComparingTo(new BigDecimal("100"));

        assertThat(totalDto.getName()).isEqualTo("totalBalance");
        assertThat(totalDto.getPercentage()).isEqualByComparingTo(new BigDecimal("18"));

        assertThat(accountDto.getName()).isEqualTo("accountBalance");
        assertThat(accountDto.getPercentage()).isEqualByComparingTo(new BigDecimal("43"));

        // Verify getSpendingAndIncomeSummary called twice with correct filters:
        ArgumentCaptor<DateRangeFilter> drfCaptor = ArgumentCaptor.forClass(DateRangeFilter.class);
        verify(serviceSpy, times(2)).getSpendingAndIncomeSummary(drfCaptor.capture());
        List<DateRangeFilter> capturedFilters = drfCaptor.getAllValues();
        // First filter: this month
        assertThat(capturedFilters.get(0).getStartDate()).isEqualTo(startThis);
        assertThat(capturedFilters.get(0).getEndDate()).isEqualTo(endThis);
        // Second filter: last month
        assertThat(capturedFilters.get(1).getStartDate()).isEqualTo(startLast);
        assertThat(capturedFilters.get(1).getEndDate()).isEqualTo(endLast);

        verify(savingsGoalRepository).getTotalBalanceByUserId(1L);
        verify(savingsGoalRepository).getLastMonthBalanceByUserId(1L);
        verify(serviceSpy).getTotalUserBalance();
        verify(transactionRepository).getAccountBalanceUpTo(1L, endLast);
    }

}
