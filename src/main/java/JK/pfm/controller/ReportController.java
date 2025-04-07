
package JK.pfm.controller;

import JK.pfm.dto.BudgetVsActualDTO;
import JK.pfm.dto.CashFlowDTO;
import JK.pfm.dto.DailyTrend;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.service.ReportService;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    
    //getting total spending and expenses
    public ResponseEntity<Map<String, BigDecimal>> getSpendingAndIncomeSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        // Option: If dates are missing, set a wide default range
        if (start == null) {
            start = LocalDate.of(1900, 1, 1);
        }
        if (end == null) {
            end = LocalDate.now();
        }
        Map<String, BigDecimal> summary = reportService.getSpendingAndIncomeSummary(start, end);
        return ResponseEntity.ok(summary);
    }
    
    /*/getting net savings
    @GetMapping("/netsavings")
     public ResponseEntity<BigDecimal> calculateNetSavings(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        BigDecimal netSavings = reportService.calculateNetSavings(start, end);
        return ResponseEntity.ok(netSavings);
    }
    */
    //getting expenses breakdown by category
    @GetMapping("/spending-by-category")
    public ResponseEntity<List<ExpenseByCategoryDTO>> getSpendingByCategory(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<ExpenseByCategoryDTO> breakdown = reportService.getSpendingByCategory(start, end);
        return ResponseEntity.ok(breakdown);
    }
    
    //get daily trend
    @GetMapping("/daily-trends")
    public ResponseEntity<List<DailyTrend>> getDailyTrends(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<DailyTrend> trends = reportService.getDailyTrends(start, end);
        return ResponseEntity.ok(trends);
    }
    
    //getting net cashflow
    public ResponseEntity<List<CashFlowDTO>> getDailyCashFlow(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<CashFlowDTO> cashFlowList = reportService.getDailyCashFlow(start, end);
        return ResponseEntity.ok(cashFlowList);
    }
    
    /*/budget vs actual spenddings
    @GetMapping("/budget-vs-actual")
    public ResponseEntity<List<BudgetVsActualDTO>> getBudgetVsActual(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<BudgetVsActualDTO> report = reportService.getBudgetVsActual(start, end);
        return ResponseEntity.ok(report);
    }
    */
}

