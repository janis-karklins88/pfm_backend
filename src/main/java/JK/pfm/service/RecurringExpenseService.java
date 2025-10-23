package JK.pfm.service;

import JK.pfm.dto.UpdatePaymentNextDueDateDto;
import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.UpdatePaymentAmountDto;
import JK.pfm.dto.UpdateRecurringExpenseAccountDto;
import JK.pfm.dto.filters.ReccurringExpenseFilter;
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
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AccountUtil accountUtil;

    public RecurringExpenseService(
        RecurringExpenseRepository recurringExpenseRepository,
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        AccountUtil accountUtil
    ) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.accountUtil = accountUtil;
    }
    
        /**
	 * Retrieves recurring expenses for the current user using optional filters.
	 * <p>
	 * Applies ownership plus optional date, category, and account filters via specifications.
	 *
	 * @param filter optional filters for date range, category, and account
	 * @return a list of matching {@link JK.pfm.model.RecurringExpense} (never {@code null})
	 */
        public List<RecurringExpense> getRecurringExpensesByFilters (ReccurringExpenseFilter filter) {
        Specification<RecurringExpense> spec = Specification.where(
            RecurringExpenseSpecifications.belongsToUser(SecurityUtil.getUserId())
        );

        // Build the specification
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
        spec = spec.and(
            RecurringExpenseSpecifications.dateBetween(
                filter.getStartDate(), 
                filter.getEndDate()
            )
        );
        } else if (filter.getStartDate() != null) {
            spec = spec.and(
                RecurringExpenseSpecifications.dateGreaterThanOrEqual(
                    filter.getStartDate()
                )
            );
        } else if (filter.getEndDate() != null) {
            spec = spec.and(
                RecurringExpenseSpecifications.dateLessThanOrEqual(
                    filter.getEndDate()
                )
            );
        }

        if (filter.getCategoryId() != null) {
            spec = spec.and(
                RecurringExpenseSpecifications.categoryEquals(
                    filter.getCategoryId()
                )
            );
        }

        if (filter.getAccountId() != null) {
            spec = spec.and(
                RecurringExpenseSpecifications.accountEquals(
                    filter.getAccountId()
                )
            );
        }     
        List<RecurringExpense> expenses = recurringExpenseRepository.findAll(spec);
        if (expenses == null) {
            expenses = new ArrayList<>();
        }        
        return expenses;
}
    
    /**
     * Creates a new recurring expense for the current user.
     *
     * @param request the creation payload (name, amount, start date, frequency, account name, category id)
     * @return the persisted {@link JK.pfm.model.RecurringExpense}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the referenced account (404 NOT FOUND) or category (404 NOT FOUND) does not exist
     */
    @Transactional
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
    
    /**
     * Returns the next five upcoming recurring expenses across the user's active accounts.
     *
     * @return up to five upcoming {@link JK.pfm.model.RecurringExpense} ordered by next due date
     */
    public List<RecurringExpense> getUpcommingRecurringExpense() {
        LocalDate todaysDate = LocalDate.now();
        List<Long> accountIds = accountUtil.getUserAccountIds();
        if (accountIds.isEmpty()) {
        return Collections.emptyList();
        }
        return recurringExpenseRepository.findTop5ByAccountIdInAndNextDueDateAfterAndActiveTrueOrderByNextDueDateAsc(accountIds, todaysDate);
    }
    
    /**
     * Updates the amount of a recurring expense.
     *
     * @param id the recurring expense ID
     * @param request the payload containing the new amount
     * @return the updated {@link JK.pfm.model.RecurringExpense}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the payment is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserAutoPay(#id)")
    public RecurringExpense updateRecurringExpenseAmount(Long id, UpdatePaymentAmountDto request){
        
        
         RecurringExpense payment = recurringExpenseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment is missing"
            ));

        payment.setAmount(request.getAmount());
        return recurringExpenseRepository.save(payment);
    }
    
    /**
     * Updates the next due date of a recurring expense.
     *
     * @param id the recurring expense ID
     * @param date the payload containing the next due date
     * @return the updated {@link JK.pfm.model.RecurringExpense}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the payment is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserAutoPay(#id)")
    public RecurringExpense updateRecurringExpenseNextDueDate(Long id, UpdatePaymentNextDueDateDto date){

        RecurringExpense payment = recurringExpenseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment is missing"
            ));

        payment.setNextDueDate(date.getNextDueDate());
        return recurringExpenseRepository.save(payment);
    }
    
    
    /**
     * Changes the funding account of a recurring expense.
     *
     * @param id the recurring expense ID
     * @param request the payload containing the new account ID
     * @return the updated {@link JK.pfm.model.RecurringExpense}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the account is invalid or inactive (404 NOT FOUND) or the payment is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserAutoPay(#id)")
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

        expense.setAccount(account);
        return recurringExpenseRepository.save(expense);
    }
    
    /**
    * Pauses a recurring expense (marks it inactive).
     *
     * @param id the recurring expense ID
     * @return the updated {@link JK.pfm.model.RecurringExpense} with {@code active=false}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the payment is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserAutoPay(#id)")
    @Transactional
    public RecurringExpense pauseRecurringExpense(Long id) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Incorrect payment"
            ));

        expense.setActive(false);
        return recurringExpenseRepository.save(expense);
    }

    /**
     * Resumes a recurring expense and recalculates the next due date based on last payment and frequency.
     * <p>
     * Advances the due date in steps (weekly/monthly/annually) until it falls after today, then activates the expense.
     *
     * @param id the recurring expense ID
     * @return the updated {@link JK.pfm.model.RecurringExpense} with a recalculated next due date
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the payment is not found (404 NOT FOUND) or the frequency is unsupported (400 BAD REQUEST)
     */
    @PreAuthorize("@securityUtil.isCurrentUserAutoPay(#id)")
    @Transactional
    public RecurringExpense resumeRecurringExpense(Long id) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment not found"
            ));
        
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

    
    /**
     * Deletes a recurring expense by ID.
     *
     * @param id the recurring expense ID
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the payment is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserAutoPay(#id)")
    public void deleteRecurringExpense(Long id) {
        RecurringExpense expense = recurringExpenseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment not found"
            ));

        recurringExpenseRepository.deleteById(id);

    }
    
    /**
     * Calculates the next due date from the current due date and frequency.
     *
     * @param expense the recurring expense
     * @return the computed next due date
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the frequency is unsupported (400 BAD REQUEST)
     */
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported frequency");
        }
    }
    
    /**
     * Processes all due (or overdue) recurring expenses and advances their schedules.
     * <p>
     * For each active expense due today or earlier, creates an expense transaction, updates the account balance,
     * sets {@code lastPayment} to today, and moves {@code nextDueDate} forward based on frequency.
     * Scheduled by cron expression {@code 0 0 0 * * ?} .
     */
    @Scheduled(cron = "0 0 0 * * ?") // every day at midnight
    @Transactional
    public void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        // Fetch recurring expenses that are due today or before today

        for (RecurringExpense expense : recurringExpenseRepository.findByNextDueDateLessThanEqualAndActiveTrue(today)) {
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
