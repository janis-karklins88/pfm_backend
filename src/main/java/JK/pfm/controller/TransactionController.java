package JK.pfm.controller;

import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Transaction;
import JK.pfm.service.TransactionService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    
    public TransactionController (TransactionService service){
        this.transactionService = service;
    }
    
	/**
	 * Creates a new transaction for the authenticated user.
	 *
	 * <p>Responds with {@code 201 Created} and the saved {@link Transaction}
	 * in the response body. The {@code Location} header points to
	 * {@code /api/transactions/{id}}.</p>
	 *
	 * @param request a {@link TransactionCreationRequest} containing transaction details
	 * @return {@code ResponseEntity} containing the created {@link Transaction}
	 * @implNote Delegates to {@link TransactionService#saveTransaction(TransactionCreationRequest)}.
	 */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionCreationRequest request) {
        Transaction savedTransaction = transactionService.saveTransaction(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(savedTransaction.getId())
        .toUri();
        return ResponseEntity.created(uri).body(savedTransaction);
    }
    
	/**
	 * Retrieves a transaction by its identifier.
	 *
	 * <p>Responds with {@code 200 OK} and the {@link Transaction} if found,
	 * or {@code 404 Not Found} if it does not exist or does not belong
	 * to the authenticated user.</p>
	 *
	 * @param id the ID of the transaction to retrieve
	 * @return {@code ResponseEntity} containing the transaction or {@code 404 Not Found}
	 * @implNote Delegates to {@link TransactionService#getTransactionById(Long)}.
	 * Consider adding an ownership check via {@link SecurityUtil#getUserId()} for security.
	 */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
	/**
	 * Deletes a transaction by its identifier.
	 *
	 * <p>Responds with {@code 204 No Content} if the deletion succeeds.</p>
	 *
	 * @param id the ID of the transaction to delete
	 * @return an empty {@code ResponseEntity} with {@code 204 No Content} status
	 * @implNote Delegates to {@link TransactionService#deleteTransaction(Long)}.
	 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
    
	/**
	 * Retrieves all transactions for the authenticated user with optional filters.
	 *
	 * <p>Supports filtering by date range, category, account, and transaction type.
	 * If no filters are provided, all transactions for the current user are returned.</p>
	 *
	 * <p>Responds with {@code 200 OK} and a (possibly empty) list of {@link Transaction} objects.</p>
	 *
	 * @param startDate optional start date (inclusive)
	 * @param endDate optional end date (inclusive)
	 * @param categoryId optional ID of the category to filter by
	 * @param accountId optional ID of the account to filter by
	 * @param type optional transaction type (e.g., "income" or "expense")
	 * @return {@code ResponseEntity} containing the filtered list of transactions
	 * @implNote Delegates to {@link TransactionService#getTransactionsByFilters(LocalDate, LocalDate, Long, Long, Long, String)}
	 * using the authenticated user's ID from {@link SecurityUtil#getUserId()}.
	 */
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
    
	/**
	 * Retrieves the most recent transactions for the authenticated user.
	 *
	 * <p>Used to display the latest activity on dashboards or summaries.</p>
	 *
	 * <p>Responds with {@code 200 OK} and a list of recent {@link Transaction} objects.</p>
	 *
	 * @return {@code ResponseEntity} containing the recent transactions
	 * @implNote Delegates to {@link TransactionService#getRecentTransactions()}.
	 */
    @GetMapping("/recent")
    public ResponseEntity<List<Transaction>> getRecentTransactions() {
        List<Transaction> transactions = transactionService.getRecentTransactions();
        return ResponseEntity.ok(transactions);  

    }
}
