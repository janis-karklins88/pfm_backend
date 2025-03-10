package JK.pfm.controller;

import JK.pfm.model.Budget;
import JK.pfm.service.BudgetService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;
    
    //get Budget
    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets() {
        List<Budget> budgets = budgetService.getAllBudgets();
        return ResponseEntity.ok(budgets);
    }

    //create budget
    @PostMapping
    public ResponseEntity<Budget> createBudget(@RequestBody Budget budget) {
        Budget savedBudget = budgetService.saveBudget(budget);
        return ResponseEntity.ok(savedBudget);
    }

    //delete budget
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
    
    //update amount
    @PatchMapping("/{id}/amount")
    public ResponseEntity<Budget> updateBudgetAmount(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal newAmount = request.get("amount");
        if (newAmount == null) {
            return ResponseEntity.badRequest().build();
        }
        Budget updatedBudget = budgetService.updateBudgetAmount(id, newAmount);
        return ResponseEntity.ok(updatedBudget);
    }
    
    //get amount spent on budget
    @GetMapping("/spent/{id}")
    public ResponseEntity<BigDecimal> getTotalSpentOnBudget(@PathVariable Long id){
        BigDecimal totalSpent = budgetService.getTotalSpentOnBudget(id);
        return ResponseEntity.ok(totalSpent);
    }
}
