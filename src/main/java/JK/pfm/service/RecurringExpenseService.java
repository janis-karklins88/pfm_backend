package JK.pfm.service;

import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.AccountRepository;
import JK.pfm.model.Account;
import JK.pfm.specifications.RecurringExpenseSpecifications;
import JK.pfm.specifications.TransactionSpecifications;
import JK.pfm.util.Validations;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

@Service
public class RecurringExpenseService {

    @Autowired
    private RecurringExpenseRepository recurringExpenseRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    // Get all recurring expenses
    public List<RecurringExpense> getRecurringExpensesByFilters(LocalDate startDate, LocalDate endDate, Long categoryId, Long accountId, Long userId) {
        Specification<RecurringExpense> spec = Specification.where(null);

    // Build the specification using our reusable methods
        if (startDate != null && endDate != null) {
            spec = spec.and(RecurringExpenseSpecifications.dateBetween(startDate, endDate));
        } else if (startDate != null) {
            spec = spec.and(RecurringExpenseSpecifications.dateGreaterThanOrEqual(startDate));
        } else if (endDate != null) {
            spec = spec.and(RecurringExpenseSpecifications.dateLessThanOrEqual(endDate));
        }

        if (categoryId != null) {
            spec = spec.and(RecurringExpenseSpecifications.categoryEquals(categoryId));
        }

        if (accountId != null) {
            spec = spec.and(RecurringExpenseSpecifications.accountEquals(accountId));
        }
    
        // Always restrict transactions to the authenticated user
        spec = spec.and(RecurringExpenseSpecifications.belongsToUser(userId));
    
        return recurringExpenseRepository.findAll(spec);
}
    
    // Save or update a recurring expense
    public RecurringExpense saveRecurringExpense(RecurringExpense expense) {
        Validations.emptyFieldValidation(expense.getName(), "name");
        Validations.numberCheck(expense.getAmount(), "amount");
        Validations.negativeCheck(expense.getAmount(), "amount");
        Validations.checkDate(expense.getStartDate());
        Validations.emptyFieldValidation(expense.getFrequency(), "frequency");
        Validations.checkObj(expense.getAccount(), "account");
        Validations.checkObj(expense.getCategory(), "category");
        return recurringExpenseRepository.save(expense);
    }
    
    // Get a recurring expense by its ID
    public Optional<RecurringExpense> getRecurringExpenseById(Long id) {
        return recurringExpenseRepository.findById(id);
    }
    
    // Delete a recurring expense by ID
    public boolean deleteRecurringExpense(Long id, Long userId) {
        Optional<RecurringExpense> recExpOpt = recurringExpenseRepository.findById(id);
        if(recExpOpt.isEmpty()){
            throw new RuntimeException("Incorrect expense!");
        }
        RecurringExpense recurringExpense = recExpOpt.get();
        if (!recurringExpense.getAccount().getUser().getId().equals(userId)) {
            // If not, return false (or throw an exception)
            return false;
        }
        recurringExpenseRepository.deleteById(id);
        return true;
    }
    
    //get next due date from frequency
    private LocalDate calculateNextDueDate(RecurringExpense expense){
        LocalDate currentDueDate = expense.getNextDueDate();
        switch (expense.getFrequency()) {
            case "WEEKLY":
                return currentDueDate.plusWeeks(1);
            case "MONTHLY":
                return currentDueDate.plusMonths(1);
            case "ANNUALLY":
                return currentDueDate.plusYears(1);
            default:
                throw new RuntimeException("Unsupported frequency: " + expense.getFrequency());
        }
    }
    
    //processing recurring expenses
    @Scheduled(cron = "0 0 * * * ?") // every day at midnight
    @Transactional
    public void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        // Fetch recurring expenses that are due today or before today

        for (RecurringExpense expense : recurringExpenseRepository.findByNextDueDateLessThanEqual(today)) {
            Account account = expense.getAccount();
            BigDecimal amount = expense.getAmount();
            
            // Create a new expense transaction
            Transaction transaction = new Transaction();
            transaction.setAccount(account);
            transaction.setAmount(amount);
            transaction.setType("Expense");
            transaction.setDate(LocalDate.now());
            transaction.setDescription("Recurring expense: " + expense.getName());
            transactionRepository.save(transaction);
            
            // Update account balance (check for sufficient funds if needed)
            account.setAmount(account.getAmount().subtract(amount));
            accountRepository.save(account);
            
            // Update the recurring expense's nextDueDate based on its frequency
            expense.setNextDueDate(calculateNextDueDate(expense));
            recurringExpenseRepository.save(expense);
        }
    }
}
