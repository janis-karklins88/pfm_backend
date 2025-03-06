package JK.pfm.controller;

import JK.pfm.model.RecurringExpense;
import JK.pfm.service.RecurringExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    @Autowired
    private RecurringExpenseService recurringExpenseService;
    
    // Retrieve all recurring expenses
    @GetMapping
    public ResponseEntity<List<RecurringExpense>> getAllRecurringExpenses() {
        List<RecurringExpense> expenses = recurringExpenseService.getAllRecurringExpenses();
        return ResponseEntity.ok(expenses);
    }
    
    // Create a new recurring expense
    @PostMapping
    public ResponseEntity<RecurringExpense> createRecurringExpense(@RequestBody RecurringExpense expense) {
        RecurringExpense savedExpense = recurringExpenseService.saveRecurringExpense(expense);
        return ResponseEntity.ok(savedExpense);
    }
    
    // Retrieve a recurring expense by ID
    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpense> getRecurringExpenseById(@PathVariable Long id) {
        return recurringExpenseService.getRecurringExpenseById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete a recurring expense by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(id);
        return ResponseEntity.noContent().build();
    }
}
