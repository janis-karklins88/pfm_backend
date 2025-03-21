package JK.pfm.service;

import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.AccountRepository;
import JK.pfm.model.Account;
import JK.pfm.util.Validations;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RecurringExpenseService {

    @Autowired
    private RecurringExpenseRepository recurringExpenseRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    // Get all recurring expenses
    public List<RecurringExpense> getAllRecurringExpenses() {
        return recurringExpenseRepository.findAll();
    }
    
    // Save or update a recurring expense
    public RecurringExpense saveRecurringExpense(RecurringExpense expense) {
        Validations.emptyFieldValidation(expense.getName(), "name");
        Validations.numberCheck(expense.getAmount(), "amount");
        Validations.negativeCheck(expense.getAmount(), "amount");
        Validations.checkDate(expense.getStartDate());
        Validations.checkDate(expense.getNextDueDate());
        Validations.emptyFieldValidation(expense.getFrequency(), "frequency");
        Validations.checkObj(expense.getAccount(), "account");
        return recurringExpenseRepository.save(expense);
    }
    
    // Get a recurring expense by its ID
    public Optional<RecurringExpense> getRecurringExpenseById(Long id) {
        return recurringExpenseRepository.findById(id);
    }
    
    // Delete a recurring expense by ID
    public void deleteRecurringExpense(Long id) {
        recurringExpenseRepository.deleteById(id);
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
    @Scheduled(cron = "0 0 0 * * ?") // every day at midnight
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
