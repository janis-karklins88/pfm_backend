package JK.pfm.service;

import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.AccountRepository;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.specifications.RecurringExpenseSpecifications;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collections;
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
        Validations.checkStartEndDate(startDate, endDate);
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
    
    // Get a next 3 payments
    public List<RecurringExpense> getUpcommingRecurringExpense() {
        Long userId = SecurityUtil.getUserId();
        LocalDate todaysDate = LocalDate.now();
        
                List<Account> accounts = accountRepository.findByUserIdAndActiveTrue(userId);
        List<Long> accountIds = new ArrayList<>();
        for(Account acc : accounts){
            Long id = acc.getId();
            accountIds.add(id);
        }
        if (accountIds.isEmpty()) {
        return Collections.emptyList();
        }
        return recurringExpenseRepository.findTop5ByAccountIdInAndNextDueDateAfterOrderByNextDueDateAsc(accountIds, todaysDate);
    }
    
    //Update amount
    public RecurringExpense updateRecurringExpenseAmount(Long id, BigDecimal amount){
        Validations.numberCheck(amount, "Amount");
        Validations.negativeCheck(amount, "Amount");
        Long userId = SecurityUtil.getUserId();
        
        Optional<RecurringExpense> expOpt = recurringExpenseRepository.findById(id);
        if(expOpt.isEmpty()){
            throw new RuntimeException("Incorrect automatic payment!");
        }
        RecurringExpense recurringExpense = expOpt.get();
        if (!recurringExpense.getAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Incorrect automatic payment!");
        }
        recurringExpense.setAmount(amount);
        return recurringExpenseRepository.save(recurringExpense);
    }
    
    //update date
    public RecurringExpense updateRecurringExpenseNextDueDate(Long id, LocalDate date){
        Validations.checkDate(date);

        Long userId = SecurityUtil.getUserId();
        
        Optional<RecurringExpense> expOpt = recurringExpenseRepository.findById(id);
        if(expOpt.isEmpty()){
            throw new RuntimeException("Incorrect automatic payment!");
        }
        RecurringExpense recurringExpense = expOpt.get();
        if (!recurringExpense.getAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Incorrect automatic payment!");
        }
        recurringExpense.setNextDueDate(date);
        return recurringExpenseRepository.save(recurringExpense);
    }
    
    
    //change account
    public RecurringExpense updateRecurringExpenseAccount(Long id, Long accountName){
        Long userId = SecurityUtil.getUserId();
        Account account = accountRepository.findByUserIdAndIdAndActiveTrue(userId, accountName).orElseThrow(() -> new RuntimeException("Incorrect account!"));
        RecurringExpense expense = recurringExpenseRepository.findById(id).orElseThrow(() -> new RuntimeException("Incorrect payment!"));
        
        if (!expense.getAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Incorrect automatic payment!");
        }
        expense.setAccount(account);
        return recurringExpenseRepository.save(expense);
    }
    
    //pause/resume
    @Transactional
    public RecurringExpense pauseRecurringExpense(Long id, Long userId) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recurring expense not found!"));
        if (!expense.getAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to pause this expense");
        }
        expense.setActive(false);
        return recurringExpenseRepository.save(expense);
    }

    //resume, inject java.time.Clock for unit testing
    @Transactional
    public RecurringExpense resumeRecurringExpense(Long id, Long userId) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recurring expense not found!"));
        if (!expense.getAccount().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to resume this expense");
        }
        
        LocalDate base = expense.getLastPayment();
        
        if(base != null){
        TemporalAmount step = switch (expense.getFrequency()) {
        case "WEEKLY"    -> Period.ofWeeks(1);
        case "MONTHLY"   -> Period.ofMonths(1);
        case "ANNUALLY"  -> Period.ofYears(1);
        default -> throw new RuntimeException("Unsupported frequency");
        };
        
        LocalDate next = base.plus(step);
        while (!next.isAfter(LocalDate.now())) {
        next = next.plus(step);
        }
        expense.setNextDueDate(next);
        }
        expense.setActive(true);
        
        return recurringExpenseRepository.save(expense);
    }

    
    // Delete a recurring expense by ID
    public boolean deleteRecurringExpense(Long id, Long userId) {
        Optional<RecurringExpense> recExpOpt = recurringExpenseRepository.findById(id);
        if(recExpOpt.isEmpty()){
            throw new RuntimeException("Incorrect payment!");
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
        String frequency = expense.getFrequency().toUpperCase();
        switch (frequency) {
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
            Category category = expense.getCategory();
            String frequency = expense.getFrequency();
            String name = expense.getName();
            
            // Create a new expense transaction
            Transaction transaction = new Transaction();
            transaction.setAccount(account);
            transaction.setAmount(amount);
            transaction.setCategory(category);
            transaction.setType("Expense");
            transaction.setDate(today);
            transaction.setDescription(frequency + " payment: " + name);
            transactionRepository.save(transaction);
            
            // Update account balance (check for sufficient funds if needed)
            account.setAmount(account.getAmount().subtract(amount));
            accountRepository.save(account);
            
            // Update the recurring expense's nextDueDate based on its frequency
            expense.setNextDueDate(calculateNextDueDate(expense));
            expense.setLastPayment(today);
            recurringExpenseRepository.save(expense);
        }
    }
}
