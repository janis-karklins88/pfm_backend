package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.specifications.TransactionSpecifications;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired 
    private AccountUtil accountUtil;
    
    
    // Save a new transaction with validations and account balance updates
    @Transactional
    public Transaction saveTransaction(Transaction transaction) {
        Account account = transaction.getAccount();
    
        // Validations
        Validations.numberCheck(transaction.getAmount(), "Amount");
        Validations.negativeCheck(transaction.getAmount(), "Amount");
        Validations.checkDate(transaction.getDate());
        Validations.checkObj(account, "account");
        Validations.checkObj(transaction.getCategory(), "category");
    
        // Transaction type validation
        if (!transaction.getType().equals("Deposit") && !transaction.getType().equals("Expense")) {
            throw new RuntimeException("Incorrect transaction type!");
        }
    
        // Update the account balance based on the transaction type
        if (transaction.getType().equals("Expense")) {
            // Check for sufficient funds
            if (account.getAmount().compareTo(transaction.getAmount()) < 0) {
                throw new RuntimeException("Insufficient funds");
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
    @Transactional
    public boolean deleteTransaction(Long id, Long userId) {
               
        Transaction transaction = transactionRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Transaction not found!"));
        
        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Transaction not found!");
        }
        Category category = transaction.getCategory();
        
        if(category.getName().equals("Savings") || 
           category.getName().equals("Fund Transfer") || 
           category.getName().equals("Initial account opening")){
           throw new RuntimeException("Can not delete this transaction!");
        }
        
        //fund handling
        Account account = transaction.getAccount();
        // Revert the account balance change before deletion
        if (transaction.getType().equals("Expense")) {
            account.setAmount(account.getAmount().add(transaction.getAmount()));
        } else {
            if (account.getAmount().compareTo(transaction.getAmount()) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
            account.setAmount(account.getAmount().subtract(transaction.getAmount()));
        }
        transactionRepository.deleteById(id);
        return true;
    }
    
    // Get transactions by filters including a filter for the authenticated user
    public List<Transaction> getTransactionsByFilters
        (LocalDate startDate, LocalDate endDate, Long categoryId, Long accountId,  Long userId, String type) {
        Specification<Transaction> spec = Specification.where(null);

        Validations.checkStartEndDate(startDate, endDate);
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
