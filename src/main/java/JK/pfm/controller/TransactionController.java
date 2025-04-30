package JK.pfm.controller;

import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.service.TransactionService;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;
    
    @Autowired 
    private AccountRepository accountRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    

    
    // Create transaction
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody TransactionCreationRequest request) {
        // Retrieve the authenticated user details
        Long userId = SecurityUtil.getUserId();
        
        // Lookup category
        Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new RuntimeException("Category not found!"));

        
        // Lookup account belonging to the authenticated user
        Validations.emptyFieldValidation(request.getAccountName(), "Account");
        Account account = accountRepository.findByUserIdAndNameAndActiveTrue(userId, request.getAccountName())
        .orElseThrow(() -> new RuntimeException("Account not found!"));
        
        // Create and save the transaction
        Transaction transaction = new Transaction(
                request.getDate(), 
                request.getAmount(), 
                account, 
                category, 
                request.getType(), 
                request.getDescription());
        Transaction savedTransaction = transactionService.saveTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction);
    }
    
    // Get transaction by ID (consider adding a check that the transaction belongs to the authenticated user)
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Delete transaction
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        Long userId = SecurityUtil.getUserId();    
        // Try to delete the transaction for this user
        boolean deleted = transactionService.deleteTransaction(id, userId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }
    
    // Get transactions with optional filters (date range, category, account) and filter by authenticated user
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String type) {
        
        // Retrieve user id from authentication
        Long userId = SecurityUtil.getUserId();
        
        List<Transaction> transactions = transactionService.getTransactionsByFilters(startDate, endDate, categoryId, accountId, userId, type);
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        return ResponseEntity.ok(transactions);
    }
    
    //recent transaction list
    @GetMapping("/recent")
    public ResponseEntity<List<Transaction>> getRecentTransactions() {
        List<Transaction> transactions = transactionService.getRecentTransactions();
        return ResponseEntity.ok(transactions);  

    }
}
