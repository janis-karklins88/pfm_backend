package JK.pfm.service;

import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.specifications.TransactionSpecifications;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Collections;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountUtil accountUtil;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountUtil accountUtil,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountUtil           = accountUtil;
        this.accountRepository     = accountRepository;
        this.categoryRepository    = categoryRepository;
    }
    
    
    /**
    * Save a new transaction for the authenticated user, applying validations and updating the
    * linked account balance atomically.
    *
    * <p>Rules:
    * <ul>
    *   <li>Account must exist, be active, and belong to the authenticated user.</li>
    *   <li>Category must exist.</li>
    *   <li>Type "Expense" subtracts from account balance and requires sufficient funds.</li>
    *   <li>Type "Deposit" adds to account balance.</li>
    * </ul>
    *
    * <p>This method is transactional; the transaction and balance update succeed or fail together.
    *
    * @param request payload containing date, amount, account name, category id, type, and description
    * @return the persisted {@link Transaction}
    * @throws org.springframework.web.server.ResponseStatusException
    *         NOT_FOUND if account or category is missing; CONFLICT if insufficient funds
    */
    @Transactional
    public Transaction saveTransaction(TransactionCreationRequest request) {
        // Retrieve the authenticated user details
        Long userId = SecurityUtil.getUserId();
        
        //Lookup account
        Account account = accountRepository.findByUserIdAndNameAndActiveTrue(userId, request.getAccountName())
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account missing"
            ));
            
        // Lookup category
        Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category missing"
            ));
        
        // Create transaction
        Transaction transaction = new Transaction(
                request.getDate(), 
                request.getAmount(), 
                account, 
                category, 
                request.getType(), 
                request.getDescription());
    
    
        // Update the account balance based on the transaction type
        if (transaction.getType().equals("Expense")) {
            // Check for sufficient funds
            if (account.getAmount().compareTo(transaction.getAmount()) < 0) {
                throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Not enough funds"
            );
            }
            account.setAmount(account.getAmount().subtract(transaction.getAmount()));
        } else if (transaction.getType().equals("Deposit")) {
            account.setAmount(account.getAmount().add(transaction.getAmount()));
        }
    
        return transactionRepository.save(transaction);
    }
    
    /**
    * Retrieve all transactions (no filtering).
    *
    * <p>Intended for administrative or internal use. Prefer filtered queries for user-facing views.
    *
    * @return list of all {@link Transaction} entities
    */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    /**
    * Find a single transaction by its id.
    *
    * @param id the transaction id
    * @return an {@link Optional} containing the transaction if found; empty otherwise
    */
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }
    
    /**
    * Delete a transaction and revert its impact on the linked account balance.
    *
    * <p>Security: only the owner of the transaction may delete it
    * (enforced via {@code @PreAuthorize("@securityUtil.isCurrentUserTransaction(#id)")}).
    *
    * <p>Rules:
    * <ul>
    *   <li>Transactions in categories "Savings", "Fund Transfer", or "Opening Account" cannot be deleted.</li>
    *   <li>Reverts balance: "Expense" adds the amount back; "Deposit" subtracts it (requires sufficient funds).</li>
    * </ul>
    *  
    * <p>Operation is transactional; the balance adjustment and deletion occur atomically.
    *
    * @param id the transaction id to delete
    * @throws org.springframework.web.server.ResponseStatusException
    *         NOT_FOUND if transaction does not exist; BAD_REQUEST if deleting is prohibited;
    *         CONFLICT if resulting balance would be negative when reverting a deposit
    */
    @PreAuthorize("@securityUtil.isCurrentUserTransaction(#id)")
    @Transactional
    public void deleteTransaction(Long id) {
               
        Transaction transaction = transactionRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Transaction not found"
            ));
        Category category = transaction.getCategory();
        
        if(category.getName().equals("Savings") || 
           category.getName().equals("Fund Transfer") || 
           category.getName().equals("Opening Account")){
           throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Deleting prohibited"
            );
        }
        
        //fund handling
        Account account = transaction.getAccount();
        // Revert the account balance change before deletion
        if (transaction.getType().equals("Expense")) {
            account.setAmount(account.getAmount().add(transaction.getAmount()));
        } else {
            if (account.getAmount().compareTo(transaction.getAmount()) < 0) {
                throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Insufficient funds"
            );
            }
            account.setAmount(account.getAmount().subtract(transaction.getAmount()));
        }
        accountRepository.save(account);
        transactionRepository.deleteById(id);
    }
    
    /**
    * Query transactions by optional filters and restrict results to the specified user.
    *
    * <p>Supported filters (all optional): date range (inclusive), category, account, type.
    * Results are always scoped to {@code userId} and sorted by {@code date DESC}, then {@code id DESC}.
    *
    * @param startDate start of date range (inclusive), or {@code null}
    * @param endDate end of date range (inclusive), or {@code null}
    * @param categoryId category id filter, or {@code null}
    * @param accountId account id filter, or {@code null}
    * @param userId required user id to scope results
    * @param type transaction type filter (e.g., "Expense", "Deposit"), or {@code null}
    * @return list of matching {@link Transaction} entities
    */
    public List<Transaction> getTransactionsByFilters
        (LocalDate startDate, LocalDate endDate, Long categoryId, Long accountId,  Long userId, String type) {
        Specification<Transaction> spec = Specification.where(null);


    // Build the specification using our reusable methods
        if (startDate != null && endDate != null) {
            spec = spec.and(TransactionSpecifications.dateBetween(startDate, endDate));
        } else if (startDate != null) {
            spec = spec.and(TransactionSpecifications.dateGreaterThanOrEqual(startDate));
        } else if (endDate != null) {
            spec = spec.and(TransactionSpecifications.dateLessThanOrEqual(endDate));
        }

        if (categoryId != null) {
            spec = spec.and(TransactionSpecifications.categoryEquals(categoryId));
        }

        if (accountId != null) {
            spec = spec.and(TransactionSpecifications.accountEquals(accountId));
        }
        
        if (type != null) {
            spec = spec.and(TransactionSpecifications.typeEquals(type));
        }
    
        // Always restrict transactions to the authenticated user
        spec = spec.and(TransactionSpecifications.belongsToUser(userId));
        
        //sorting in desc order
        Sort sort = Sort.by(
        Sort.Order.desc("date"), 
        Sort.Order.desc("id") 
        );
    
        return transactionRepository.findAll(spec, sort);
}
    
    /**
    * Fetch the 5 most recent transactions across all active accounts for the authenticated user.
    *
    * <p>If the user has no accounts, returns an empty list.
    *
    * @return up to 5 most recent {@link Transaction} entities ordered by id descending
    */
    public List<Transaction> getRecentTransactions() {
        List<Long> accountIds = accountUtil.getUserAccountIds();
        if (accountIds.isEmpty()) {
        return Collections.emptyList();
        }
        List<Transaction> transactions = transactionRepository.findTop5ByAccountIdInOrderByIdDesc(accountIds);
        
        return transactions;
    }
}
