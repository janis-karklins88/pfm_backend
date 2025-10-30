
package JK.pfm.controller;

import JK.pfm.dto.BalanceBreakdownDTO;
import JK.pfm.dto.CashFlowDTO;
import JK.pfm.dto.ChangesVsLastMonthDTO;
import JK.pfm.dto.DailyTrend;
import JK.pfm.dto.ExpenseByAccountDTO;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.dto.filters.DateRangeFilter;
import JK.pfm.service.ReportService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    
    
    /**
     * Retrieves a summary of total income and expenses for the authenticated user.
     *
     * <p>Responds with {@code 200 OK} and a map containing two keys:
     * {@code "income"} and {@code "expenses"}, each mapped to their respective totals.</p>
     *
     * @param filter optional {@link DateRangeFilter} specifying the time period to include
     * @return {@code ResponseEntity} containing the income and expense summary
     * @implNote Delegates to {@link ReportService#getSpendingAndIncomeSummary(DateRangeFilter)}.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, BigDecimal>> getSpendingAndIncomeSummary(@Valid @ModelAttribute DateRangeFilter filter) {
        Map<String, BigDecimal> summary = reportService.getSpendingAndIncomeSummary(filter);
        return ResponseEntity.ok(summary);
    }
    

    /**
     * Returns total expenses grouped by category.
     *
     * <p>Responds with {@code 200 OK} and a list of {@link ExpenseByCategoryDTO}
     * representing each category and its corresponding total spending.</p>
     *
     * @param filter optional {@link DateRangeFilter} limiting the date range
     * @return {@code ResponseEntity} containing expenses grouped by category
     * @implNote Delegates to {@link ReportService#getSpendingByCategory(DateRangeFilter)}.
     */
    @GetMapping("/spending-by-category")
    public ResponseEntity<List<ExpenseByCategoryDTO>> getSpendingByCategory(@Valid @ModelAttribute DateRangeFilter filter) {
        return ResponseEntity.ok(reportService.getSpendingByCategory(filter));
    }
    
    /**
     * Returns total expenses grouped by account.
     *
     * <p>Responds with {@code 200 OK} and a list of {@link ExpenseByAccountDTO}
     * representing each account and the total amount spent from it.</p>
     *
     * @param filter optional {@link DateRangeFilter} limiting the date range
     * @return {@code ResponseEntity} containing expenses grouped by account
     * @implNote Delegates to {@link ReportService#getSpendingByAccount(DateRangeFilter)}.
     */
    @GetMapping("/spending-by-account")
    public ResponseEntity<List<ExpenseByAccountDTO>> getSpendingByAccount(@Valid @ModelAttribute DateRangeFilter filter) {
        return ResponseEntity.ok(reportService.getSpendingByAccount(filter));
    }
    
    /**
    * Retrieves the user's total balance across all accounts.
     *
     * <p>Responds with {@code 200 OK} and a single {@link BigDecimal} representing
     * the combined balance of all user accounts.</p>
     *
     * @return {@code ResponseEntity} containing the user's total balance
     * @implNote Delegates to {@link ReportService#getTotalUserBalance()}.
     */
    @GetMapping("balance")
    public ResponseEntity<BigDecimal> getTotalUserBalance() {
        BigDecimal sum = reportService.getTotalUserBalance();
        return ResponseEntity.ok(sum);
    }
    
    /**
     * Returns the user's daily spending and income trends within the specified range.
     *
     * <p>Responds with {@code 200 OK} and a list of {@link DailyTrend} objects representing
     * day-by-day totals.</p>
     *
     * @param filter optional {@link DateRangeFilter} limiting the date range
     * @return {@code ResponseEntity} containing the list of daily trends
     * @implNote Delegates to {@link ReportService#getDailyTrends(DateRangeFilter)}.
     */
    @GetMapping("/daily-trends")
    public ResponseEntity<List<DailyTrend>> getDailyTrends(@Valid @ModelAttribute DateRangeFilter filter) {
        return ResponseEntity.ok(reportService.getDailyTrends(filter));
    }
    
    /**
     * Retrieves monthly cash flow data for the authenticated user.
     *
     * <p>Responds with {@code 200 OK} and a list of {@link CashFlowDTO} objects
     * showing income, expenses, and net balance for each month.</p>
     *
     * @return {@code ResponseEntity} containing monthly cash flow data
     * @implNote Delegates to {@link ReportService#getMonthlyCashFlow()}.
     */
    @GetMapping("/monthly-cashflow")
    public ResponseEntity<List<CashFlowDTO>> getMonthlyCashFlow() {
            List<CashFlowDTO> cashFlowList = reportService.getMonthlyCashFlow();
        return ResponseEntity.ok(cashFlowList);
    }
    
    /**
     * Returns a breakdown of balances grouped by account or asset type.
     *
     * <p>Responds with {@code 200 OK} and a list of {@link BalanceBreakdownDTO}
     * entries showing how the total balance is distributed.</p>
     *
     * @return list of {@link BalanceBreakdownDTO} representing the balance breakdown
     * @implNote Delegates to {@link ReportService#getBalanceBreakdown()}.
     */
    @GetMapping("/balance-breakdown")
    public List<BalanceBreakdownDTO> getBalanceBreakdown(){
        List<BalanceBreakdownDTO> breakdown = reportService.getBalanceBreakdown();
        return breakdown;
    }
    
    /**
     * Retrieves user spending for the last 9 months, current month,
     * and projected spending for the next 2 months.
     *
     * <p>Responds with {@code 200 OK} and a map where keys represent month labels
     * and values represent total or predicted spending amounts.</p>
     *  	 
     * @return {@code ResponseEntity} containing the historical and predicted spending map
     * @implNote Delegates to {@link ReportService#getExpenseAndPrediction()}.
     */
    @GetMapping("/expense-and-prediction")
    public ResponseEntity<Map<String, BigDecimal>> getExpenseAndPrediction(){
        Map<String, BigDecimal> breakdown = reportService.getExpenseAndPrediction();
        return ResponseEntity.ok(breakdown);
    }
    
    /**
     * Retrieves spending amounts for a specific category over time.
     *
     * <p>If {@code categoryId} is not provided, the total across all categories is returned.</p>
     *
     * <p>Responds with {@code 200 OK} and a map of months to spending totals.</p>
     *
     * @param categoryId optional ID of the category to filter by
     * @return {@code ResponseEntity} containing spending amounts for the selected category
     * @implNote Delegates to {@link ReportService#getExpenseForCategory(Long)}.
     */
    @GetMapping("/expense-for-category")
    public ResponseEntity<Map<String, BigDecimal>> getExpenseByCategory(
        @RequestParam(required = false) Long categoryId){
        Map<String, BigDecimal> breakdown = reportService.getExpenseForCategory(categoryId);
        return ResponseEntity.ok(breakdown);
    }
    
    /**
     * Retrieves the change in balance or spending compared to the previous month.
     *
     * <p>Responds with {@code 200 OK} and a list of {@link ChangesVsLastMonthDTO}
     * entries representing differences in key financial metrics.</p>
     *
     * @return {@code ResponseEntity} containing balance and spending change data
     * @implNote Delegates to {@link ReportService#getChanges()}.
     */
    @GetMapping("/balance-change")
    public ResponseEntity<List<ChangesVsLastMonthDTO>> getAmountChanges(){
        List<ChangesVsLastMonthDTO> changes = reportService.getChanges();
        return ResponseEntity.ok(changes);
    }
}

