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
    
    
    // Save a new transaction with validations and account balance updates
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
    
    // Get all transactions (if needed)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    // Get transaction by id
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }
    
    // Delete a transaction by id, adjusting account balance before deletion
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
    
    // Get transactions by filters including a filter for the authenticated user
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
    
    //get recent transactions
    public List<Transaction> getRecentTransactions() {
        List<Long> accountIds = accountUtil.getUserAccountIds();
        if (accountIds.isEmpty()) {
        return Collections.emptyList();
        }
        List<Transaction> transactions = transactionRepository.findTop5ByAccountIdInOrderByIdDesc(accountIds);
        
        return transactions;
    }
}
