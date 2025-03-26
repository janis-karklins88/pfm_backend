package JK.pfm.controller;

import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;

import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;

import JK.pfm.security.CustomUserDetails;
import JK.pfm.service.RecurringExpenseService;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
        
        // Retrieve user id from authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        
        List<RecurringExpense> expenses = recurringExpenseService.getRecurringExpensesByFilters(startDate, endDate, categoryId, accountId, userId);
        if (expenses == null) {
            expenses = new ArrayList<>();
        }
        return ResponseEntity.ok(expenses);
    }
    
    // Create a new recurring expense
    @PostMapping
    public ResponseEntity<RecurringExpense> createRecurringExpense(@RequestBody RecurringExpenseCreation request) {
        
        //get user id
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
       
        //Lookup account
        Optional<Account> accOpt = accountRepository.findByUserIdAndName(userId, request.getAccountName());
        if (accOpt.isEmpty()){
            throw new RuntimeException("Incorrect account!");
        }
        Account account = accOpt.get();

        // Lookup category
        Optional<Category> catOpt = categoryRepository.findByName(request.getCategoryName());
        if (catOpt.isEmpty()){
            throw new RuntimeException("Incorrect category!");
        }
        Category category = catOpt.get();
        
        RecurringExpense expense = new RecurringExpense(request.getName(), request.getAmount(), request.getStartDate(), request.getFrequency(), account, category);
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
        // Get authenticated user's ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
    
        // Try to delete the budget for this user
        boolean deleted = recurringExpenseService.deleteRecurringExpense(id, userId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }
}
