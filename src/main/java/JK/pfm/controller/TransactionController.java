package JK.pfm.controller;

import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.dto.UnifiedTransactionDTO;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.service.TransactionService;
import JK.pfm.util.SecurityUtil;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Optional<Category> catOpt = categoryRepository.findByName(request.getCategoryName());
        if (catOpt.isEmpty()){
            throw new RuntimeException("Incorrect category!");
        }
        Category category = catOpt.get();
        
        // Lookup account belonging to the authenticated user 
        Optional<Account> accOpt = accountRepository.findByUserIdAndName(userId, request.getAccountName());
        if (accOpt.isEmpty()){
            throw new RuntimeException("Incorrect account!");
        }
        Account account = accOpt.get();
        
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
            @RequestParam(required = false) Long accountId) {
        
        // Retrieve user id from authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        
        List<Transaction> transactions = transactionService.getTransactionsByFilters(startDate, endDate, categoryId, accountId, userId);
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        return ResponseEntity.ok(transactions);
    }
    
    //Unified transaction list
    @GetMapping("/all")
    public ResponseEntity<List<UnifiedTransactionDTO>> getUnifiedTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId) {

        // Retrieve authenticated user's ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        List<UnifiedTransactionDTO> unifiedTransactions =
                transactionService.getUnifiedTransactions(startDate, endDate, categoryId, accountId, userId);
        if (unifiedTransactions == null) {
            unifiedTransactions = new ArrayList<>();
        }
        return ResponseEntity.ok(unifiedTransactions);
    }
}
