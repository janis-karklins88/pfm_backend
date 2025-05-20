package JK.pfm.controller;

import JK.pfm.model.Budget;
import JK.pfm.service.BudgetService;
import java.math.BigDecimal;
import JK.pfm.dto.BudgetCreationRequest;
import JK.pfm.dto.UpdateBudgetAmountDto;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    
    //get Budgets
    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filterStart,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate filterEnd) {

    Long userId = SecurityUtil.getUserId();

    List<Budget> budgets = budgetService.getAllBudgets(userId, filterStart, filterEnd);
    return ResponseEntity.ok(budgets);
}

    //create budget
    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody BudgetCreationRequest request) {
        
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.saveBudget(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudgetForUser(id);
        return ResponseEntity.noContent().build();
}

    
    //update amount
    @PatchMapping("/{id}")
    public ResponseEntity<Budget> updateBudgetAmount(@PathVariable Long id, 
            @Valid @RequestBody UpdateBudgetAmountDto request) {
        return ResponseEntity.ok(budgetService.updateBudgetAmount(id, request));
    }
    
    //get amount spent on budget
    @GetMapping("/spent/{id}")
    public ResponseEntity<BigDecimal> getTotalSpentOnBudget(@PathVariable Long id){
        BigDecimal totalSpent = budgetService.getTotalSpentOnBudget(id);
        return ResponseEntity.ok(totalSpent);
    }
    
    //set monthly active/inactive
    @PatchMapping("/{budgetId}/monthly")
    public ResponseEntity<Budget> updateMonthlyStatus(@PathVariable Long budgetId, 
                                                      @RequestParam boolean active) {
        Budget budget = budgetService.updateMonthlyStatus(budgetId, active);
        return ResponseEntity.ok(budget);
    }
}
