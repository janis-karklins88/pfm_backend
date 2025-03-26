package JK.pfm.service;

import JK.pfm.dto.UnifiedTransactionDTO;
import JK.pfm.model.Account;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.specifications.TransactionSpecifications;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired 
    private RecurringExpenseService recurringExpenseService;
    
    
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
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found!");
        }
        Transaction transaction = transactionOpt.get();
        if (!transaction.getAccount().getUser().getId().equals(userId)) {
            // If not, return false (or throw an exception)
            return false;
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
    public List<Transaction> getTransactionsByFilters(LocalDate startDate, LocalDate endDate, Long categoryId, Long accountId, Long userId) {
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
    
        // Always restrict transactions to the authenticated user
        spec = spec.and(TransactionSpecifications.belongsToUser(userId));
    
        return transactionRepository.findAll(spec);
}
    
    //unified transaction list, not needed right now
    public List<UnifiedTransactionDTO> getUnifiedTransactions(LocalDate startDate, LocalDate endDate,
                                                               Long categoryId, Long accountId, Long userId) {
        // Fetch transactions and recurring expenses using existing methods
        List<Transaction> transactions = this.getTransactionsByFilters(startDate, endDate, categoryId, accountId, userId);
        List<RecurringExpense> recurringExpenses = recurringExpenseService.getRecurringExpensesByFilters(startDate, endDate, categoryId, accountId, userId);

        List<UnifiedTransactionDTO> unifiedList = new ArrayList<>();

        // Map Transactions to UnifiedTransactionDTO
        for (Transaction txn : transactions) {
            UnifiedTransactionDTO dto = new UnifiedTransactionDTO();
            dto.setId(txn.getId());
            dto.setDate(txn.getDate());  // assuming "date" is the execution date for one-off transactions
            dto.setAmount(txn.getAmount());
            dto.setCategoryName(txn.getCategory() != null ? txn.getCategory().getName() : null);
            dto.setAccountName(txn.getAccount() != null ? txn.getAccount().getName() : null);
            dto.setType(txn.getType()); // "Deposit" or "Expense"
            dto.setDescription(txn.getDescription());
            // frequency not applicable for one-off transactions
            unifiedList.add(dto);
        }

        // Map RecurringExpenses to UnifiedTransactionDTO
        for (RecurringExpense recExp : recurringExpenses) {
            UnifiedTransactionDTO dto = new UnifiedTransactionDTO();
            dto.setId(recExp.getId());
            // For recurring expenses, choose a representative date.
            // You might use the nextDueDate if available; otherwise, use startDate.
            dto.setDate(recExp.getNextDueDate() != null ? recExp.getNextDueDate() : recExp.getStartDate());
            dto.setAmount(recExp.getAmount());
            dto.setCategoryName(recExp.getCategory() != null ? recExp.getCategory().getName() : null);
            dto.setAccountName(recExp.getAccount() != null ? recExp.getAccount().getName() : null);
            // Here we label it as "Recurring Expense"
            dto.setType("Recurring Expense");
            // You can include additional recurring-specific info, such as frequency:
            dto.setFrequency(recExp.getFrequency());
            // Optionally, set description if available
            dto.setDescription(recExp.getName()); // or another field
            unifiedList.add(dto);
        }

        // Sort the unified list by date (ascending order)
        unifiedList.sort(Comparator.comparing(UnifiedTransactionDTO::getDate));

        return unifiedList;
    }
}
