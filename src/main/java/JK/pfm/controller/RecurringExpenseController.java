package JK.pfm.controller;

import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.service.RecurringExpenseService;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    @Autowired
    private RecurringExpenseService recurringExpenseService;
    @Autowired 
    private CategoryRepository categoryRepository;
    @Autowired
    private AccountRepository accountRepository;
    
    
    // Retrieve all recurring expenses
    @GetMapping
    public ResponseEntity<List<RecurringExpense>> getRecurringExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId) {
        
        Long userId = SecurityUtil.getUserId();
        
        List<RecurringExpense> expenses = recurringExpenseService.getRecurringExpensesByFilters(startDate, endDate, categoryId, accountId, userId);
        if (expenses == null) {
            expenses = new ArrayList<>();
        }
        return ResponseEntity.ok(expenses);
    }
    
    // Create a new recurring expense
    @PostMapping
    public ResponseEntity<RecurringExpense> createRecurringExpense(@RequestBody RecurringExpenseCreation request) {
        
        Long userId = SecurityUtil.getUserId();
       
        //Lookup account
        Account account = accountRepository.findByUserIdAndName(userId, request.getAccountName())
        .orElseThrow(() -> new RuntimeException("Account not found!"));
        

        // Lookup category
        Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new RuntimeException("Category not found!"));
        
        RecurringExpense expense = new RecurringExpense(request.getName(), request.getAmount(), request.getStartDate(), request.getFrequency(), account, category);
        RecurringExpense savedExpense = recurringExpenseService.saveRecurringExpense(expense);
        return ResponseEntity.ok(savedExpense);
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
        // Get authenticated user's ID
        Long userId = SecurityUtil.getUserId();
    
        boolean deleted = recurringExpenseService.deleteRecurringExpense(id, userId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }
    
    //edit amount
    @PatchMapping("/amount/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAmount(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal newAmount = request.get("amount");
        //update this later to more informative
        if (newAmount == null) {
            return ResponseEntity.badRequest().build();
        }
        RecurringExpense recurringExpense = recurringExpenseService.updateRecurringExpenseAmount(id, newAmount);
        return ResponseEntity.ok(recurringExpense);
    }
    
    //edit nextduedate
    @PatchMapping("/name/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseNextDueDate(@PathVariable Long id, @RequestBody Map<String, LocalDate> request) {
        LocalDate date = request.get("date");
        //update this later to more informative
        if (date == null) {
            return ResponseEntity.badRequest().build();
        }
        RecurringExpense recurringExpense = recurringExpenseService.updateRecurringExpenseNextDueDate(id, date);
        return ResponseEntity.ok(recurringExpense);
    }
    
    //change account
    @PatchMapping("/account/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpenseAccount(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String accountName = request.get("accountName");
        //update this later to more informative
        if (accountName == null) {
            return ResponseEntity.badRequest().build();
        }
        RecurringExpense recurringExpense = recurringExpenseService.updateRecurringExpenseAccount(id, accountName);
        return ResponseEntity.ok(recurringExpense);
    }
    
    //pause/resume
    // Pause a recurring expense
    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringExpense> pauseRecurringExpense(@PathVariable Long id) {
        Long userId = SecurityUtil.getUserId();
        RecurringExpense updatedExpense = recurringExpenseService.pauseRecurringExpense(id, userId);
        return ResponseEntity.ok(updatedExpense);
    }

    // Resume a recurring expense
    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringExpense> resumeRecurringExpense(@PathVariable Long id) {
        Long userId = SecurityUtil.getUserId();
        RecurringExpense updatedExpense = recurringExpenseService.resumeRecurringExpense(id, userId);
        return ResponseEntity.ok(updatedExpense);
    }
}
