
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

    @GetMapping("/summary")
    
    //getting total income and expenses
    public ResponseEntity<Map<String, BigDecimal>> getSpendingAndIncomeSummary(@Valid @ModelAttribute DateRangeFilter filter) {
        Map<String, BigDecimal> summary = reportService.getSpendingAndIncomeSummary(filter);
        return ResponseEntity.ok(summary);
    }
    

    //getting expenses breakdown by category
    @GetMapping("/spending-by-category")
    public ResponseEntity<List<ExpenseByCategoryDTO>> getSpendingByCategory(@Valid @ModelAttribute DateRangeFilter filter) {
        return ResponseEntity.ok(reportService.getSpendingByCategory(filter));
    }
    
    //get expense breakdown by account 
    @GetMapping("/spending-by-account")
    public ResponseEntity<List<ExpenseByAccountDTO>> getSpendingByAccount(@Valid @ModelAttribute DateRangeFilter filter) {
        return ResponseEntity.ok(reportService.getSpendingByAccount(filter));
    }
    
    //get total user balance

    @GetMapping("balance")
    public ResponseEntity<BigDecimal> getTotalUserBalance() {
        BigDecimal sum = reportService.getTotalUserBalance();
        return ResponseEntity.ok(sum);
    }
    
    //get daily trend
    @GetMapping("/daily-trends")
    public ResponseEntity<List<DailyTrend>> getDailyTrends(@Valid @ModelAttribute DateRangeFilter filter) {
        return ResponseEntity.ok(reportService.getDailyTrends(filter));
    }
    
    //getting cashflow
    @GetMapping("/monthly-cashflow")
    public ResponseEntity<List<CashFlowDTO>> getMonthlyCashFlow() {
            List<CashFlowDTO> cashFlowList = reportService.getMonthlyCashFlow();
        return ResponseEntity.ok(cashFlowList);
    }
    
    //get total balance breakdown
    @GetMapping("/balance-breakdown")
    public List<BalanceBreakdownDTO> getBalanceBreakdown(){
        List<BalanceBreakdownDTO> breakdown = reportService.getBalanceBreakdown();
        return breakdown;
    }
    
    //get spending for current + 9 last months + prediction for next 2 months
    @GetMapping("/expense-and-prediction")
    public ResponseEntity<Map<String, BigDecimal>> getExpenseAndPrediction(){
        Map<String, BigDecimal> breakdown = reportService.getExpenseAndPrediction();
        return ResponseEntity.ok(breakdown);
    }
    
    //get spending for current + 9 last months + prediction for next 2 months
    @GetMapping("/expense-for-category")
    public ResponseEntity<Map<String, BigDecimal>> getExpenseByCategory(
        @RequestParam(required = false) Long categoryId){
        Map<String, BigDecimal> breakdown = reportService.getExpenseForCategory(categoryId);
        return ResponseEntity.ok(breakdown);
    }
    
    //get amount change vs last month
    @GetMapping("/balance-change")
    public ResponseEntity<List<ChangesVsLastMonthDTO>> getAmountChanges(){
        List<ChangesVsLastMonthDTO> changes = reportService.getChanges();
        return ResponseEntity.ok(changes);
    }
}

