package JK.pfm.controller;

import JK.pfm.model.Budget;
import JK.pfm.service.BudgetService;
import java.math.BigDecimal;
import JK.pfm.dto.BudgetCreationRequest;
import JK.pfm.dto.UpdateBudgetAmountDto;
import JK.pfm.dto.filters.DateRangeFilter;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    public BudgetController(BudgetService service){
        this.budgetService = service;
    }

    
    //get Budgets
    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets(@Valid @ModelAttribute DateRangeFilter filter) {
    return ResponseEntity.ok(budgetService.getAllBudgets(filter));
}

    //create budget
    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody BudgetCreationRequest request) {
        var budget = budgetService.saveBudget(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(budget.getId())
        .toUri();
        return ResponseEntity.created(uri).body(budget);
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
