
package JK.pfm.controller;

import JK.pfm.dto.BudgetVsActualDTO;
import JK.pfm.dto.DailyTrend;
import JK.pfm.service.ReportService;
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

/**
 *
 * @author user
 */
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
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        Map<String, BigDecimal> summary = reportService.getSpendingAndIncomeSummary(start, end);
        return ResponseEntity.ok(summary);
    }
    
    //getting expenses breakdown by category
    @GetMapping("/spending-by-category")
    public ResponseEntity<Map<String, BigDecimal>> getSpendingByCategory(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        Map<String, BigDecimal> breakdown = reportService.getSpendingByCategory(start, end);
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
    
    //budget vs actual spenddings
    @GetMapping("/budget-vs-actual")
    public ResponseEntity<List<BudgetVsActualDTO>> getBudgetVsActual(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<BudgetVsActualDTO> report = reportService.getBudgetVsActual(start, end);
        return ResponseEntity.ok(report);
    }
    
}

