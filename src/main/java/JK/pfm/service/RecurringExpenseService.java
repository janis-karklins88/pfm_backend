package JK.pfm.service;

import JK.pfm.dto.UpdatePaymentNextDueDateDto;
import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.UpdatePaymentAmountDto;
import JK.pfm.dto.UpdateRecurringExpenseAccountDto;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.AccountRepository;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.specifications.RecurringExpenseSpecifications;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecurringExpenseService {

    @Autowired
    private RecurringExpenseRepository recurringExpenseRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired 
    private AccountUtil accountUtil;
    
    // Get all recurring expenses
    public List<RecurringExpense> getRecurringExpensesByFilters
        (LocalDate startDate, LocalDate endDate, Long categoryId, Long accountId, Long userId) {
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
    public RecurringExpense saveRecurringExpense(RecurringExpenseCreation request) {
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
        
        RecurringExpense expense = new RecurringExpense(
                        request.getName(), 
                        request.getAmount(), 
                        request.getStartDate(), 
                        request.getFrequency(), 
                        account, 
                        category
                        );

        return recurringExpenseRepository.save(expense);
    }
    
    // Get a next 3 payments
    public List<RecurringExpense> getUpcommingRecurringExpense() {
        LocalDate todaysDate = LocalDate.now();
        
        List<Long> accountIds = accountUtil.getUserAccountIds();
        if (accountIds.isEmpty()) {
        return Collections.emptyList();
        }
        return recurringExpenseRepository.findTop5ByAccountIdInAndNextDueDateAfterOrderByNextDueDateAsc(accountIds, todaysDate);
    }
    
    //Update amount
    public RecurringExpense updateRecurringExpenseAmount(Long id, UpdatePaymentAmountDto request){
        
        
         RecurringExpense payment = recurringExpenseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment is missing"
            ));
         
            if (!payment.getAccount().getUser().getId().equals(SecurityUtil.getUserId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Payment incorrect"
            );
        }
        payment.setAmount(request.getAmount());
        return recurringExpenseRepository.save(payment);
    }
    
    //update date
    public RecurringExpense updateRecurringExpenseNextDueDate(Long id, UpdatePaymentNextDueDateDto date){

        RecurringExpense payment = recurringExpenseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment is missing"
            ));
        
        if (!payment.getAccount().getUser().getId().equals(SecurityUtil.getUserId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Payment incorrect"
            );
        }
        payment.setNextDueDate(date.getNextDueDate());
        return recurringExpenseRepository.save(payment);
    }
    
    
    //change account
    public RecurringExpense updateRecurringExpenseAccount(Long id, UpdateRecurringExpenseAccountDto request){
        Long userId = SecurityUtil.getUserId();
        Account account = accountRepository.findByUserIdAndIdAndActiveTrue(userId, request.getAccountId())
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Incorrect account"
            ));
        
        RecurringExpense expense = recurringExpenseRepository.findById(id).
                orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Incorrect payment"
            ));
        
        if (!expense.getAccount().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Incorrect payment"
            );
        }
        expense.setAccount(account);
        return recurringExpenseRepository.save(expense);
    }
    
    //pause
    @Transactional
    public RecurringExpense pauseRecurringExpense(Long id) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Incorrect payment"
            ));
        
        if (!expense.getAccount().getUser().getId().equals(SecurityUtil.getUserId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Incorrect payment"
            );
        }
        expense.setActive(false);
        return recurringExpenseRepository.save(expense);
    }

    //resume, inject java.time.Clock for unit testing
    @Transactional
    public RecurringExpense resumeRecurringExpense(Long id) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment not found"
            ));
        
        if (!expense.getAccount().getUser().getId().equals(SecurityUtil.getUserId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Incorrect payment"
            );
        }
        
        LocalDate base = expense.getLastPayment();
        
        if(base != null){
        TemporalAmount step = switch (expense.getFrequency()) {
        case "WEEKLY"    -> Period.ofWeeks(1);
        case "MONTHLY"   -> Period.ofMonths(1);
        case "ANNUALLY"  -> Period.ofYears(1);
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported frequency");
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
    public void deleteRecurringExpense(Long id) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment not found"
            ));
        
        if (!expense.getAccount().getUser().getId().equals(SecurityUtil.getUserId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Incorrect payment"
            );
        }
        recurringExpenseRepository.deleteById(id);

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
