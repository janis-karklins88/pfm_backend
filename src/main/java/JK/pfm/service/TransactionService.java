package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.model.Transaction;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    
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
    public void deleteTransaction(Long id) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found!");
        }
        Transaction transaction = transactionOpt.get();
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
    }
    
    // Get transactions by filters including a filter for the authenticated user
    public List<Transaction> getTransactionsByFilters(LocalDate startDate, LocalDate endDate, Long categoryId, Long accountId, Long userId) {
        // Start with an empty Specification
        Specification<Transaction> spec = Specification.where(null);

        // Filter by date range if provided
        if (startDate != null && endDate != null) {
            spec = spec.and((root, query, cb) -> cb.between(root.get("date"), startDate, endDate));
        } else if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), startDate));
        } else if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), endDate));
        }

        // Filter by category if provided
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        // Filter by account if provided
        if (accountId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("account").get("id"), accountId));
        }
        
        // Filter by the authenticated user's id (via the account relationship)
        spec = spec.and((root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId));
        
        return transactionRepository.findAll(spec);
    }
}
