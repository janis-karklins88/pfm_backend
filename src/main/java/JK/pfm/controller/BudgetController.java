package JK.pfm.controller;

import JK.pfm.model.Budget;
import JK.pfm.service.BudgetService;
import java.math.BigDecimal;
import JK.pfm.dto.BudgetCreationRequest;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.SecurityUtil;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    
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
    public ResponseEntity<Budget> createBudget(@RequestBody BudgetCreationRequest request) {

        User user = SecurityUtil.getUser(userRepository);
        
        //get category
        Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new RuntimeException("Category not found!"));
        
        Budget budget = new Budget(request.getAmount(), request.getStartDate(), request.getEndDate(), category, user);
        
        Budget savedBudget = budgetService.saveBudget(budget);
        return ResponseEntity.ok(savedBudget);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        Long userId = SecurityUtil.getUserId();
    
        // Try to delete the budget for this user
        boolean deleted = budgetService.deleteBudgetForUser(id, userId);
        if (!deleted) {
            // If deletion fails because the budget doesn't belong to the user, return 403 Forbidden
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
}

    
    //update amount
    @PatchMapping("/{id}")
    public ResponseEntity<Budget> updateBudgetAmount(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal newAmount = request.get("amount");

        Budget updatedBudget = budgetService.updateBudgetAmount(id, newAmount);
        return ResponseEntity.ok(updatedBudget);
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
