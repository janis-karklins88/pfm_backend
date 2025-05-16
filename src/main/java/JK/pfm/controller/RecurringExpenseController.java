package JK.pfm.controller;

import JK.pfm.dto.UpdatePaymentNextDueDateDto;
import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.UpdatePaymentAmountDto;
import JK.pfm.dto.UpdateRecurringExpenseAccountDto;
import JK.pfm.model.RecurringExpense;
import JK.pfm.service.RecurringExpenseService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    @Autowired
    private RecurringExpenseService recurringExpenseService;
    
    
    // Retrieve all recurring expenses
    @GetMapping
    public ResponseEntity<List<RecurringExpense>> getRecurringExpenses(
            @RequestParam(name = "startDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
                    LocalDate startDate,
            
            @RequestParam(name = "endDate", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
                    LocalDate endDate,
            
            @RequestParam(name = "categoryId", required = false) 
                    Long categoryId,
            
            @RequestParam(name = "accountId", required = false) 
                    Long accountId) {
        
        Long userId = SecurityUtil.getUserId();
        
        List<RecurringExpense> expenses = recurringExpenseService.getRecurringExpensesByFilters(startDate, endDate, categoryId, accountId, userId);
        if (expenses == null) {
            expenses = new ArrayList<>();
        }
        return ResponseEntity.ok(expenses);
    }
    
    // Create a new recurring expense
    @PostMapping
    public ResponseEntity<RecurringExpense> createRecurringExpense(@Valid @RequestBody RecurringExpenseCreation request) {
        return ResponseEntity.ok(recurringExpenseService.saveRecurringExpense(request));
    }
    
    //next payments
    @GetMapping("/next-payments")
    public ResponseEntity<List<RecurringExpense>> getNextPayements() {
        List<RecurringExpense> payments = recurringExpenseService.getUpcommingRecurringExpense();
        return ResponseEntity.ok(payments);  

    }
    
    // Delete a recurring expense by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable Long id) {
        recurringExpenseService.deleteRecurringExpense(id);
        return ResponseEntity.noContent().build();
    }
    
    //edit amount
    @PatchMapping("/amount/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAmount(@PathVariable Long id,@Valid @RequestBody UpdatePaymentAmountDto request) {
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseAmount(id, request));
    }
    
    //edit nextduedate
    @PatchMapping("/name/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseNextDueDate(
            @PathVariable Long id, 
            @Valid @RequestBody UpdatePaymentNextDueDateDto request) {

        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseNextDueDate(id, request));
    }
    
    //change account
    @PatchMapping("/account/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAccount(@PathVariable Long id, @Valid @RequestBody UpdateRecurringExpenseAccountDto request) {
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpenseAccount(id, request));
    }
    
    // Pause a recurring expense
    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringExpense> pauseRecurringExpense(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.pauseRecurringExpense(id));
    }

    // Resume a recurring expense
    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringExpense> resumeRecurringExpense(@PathVariable Long id) {
        return ResponseEntity.ok(recurringExpenseService.resumeRecurringExpense(id));
    }
}
