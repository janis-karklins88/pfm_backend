package JK.pfm.controller;

import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Transaction;
import JK.pfm.service.TransactionService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    
    // Create transaction
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionCreationRequest request) {
        Transaction savedTransaction = transactionService.saveTransaction(request);
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
        transactionService.deleteTransaction(id);
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
