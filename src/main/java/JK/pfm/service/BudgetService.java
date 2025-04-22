package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.specifications.BudgetSpecifications;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;


@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    
    //getting all budgets for user
    public List<Budget> getAllBudgets(Long userId, LocalDate filterStart, LocalDate filterEnd) {
    // Start with the specification that filters budgets by user
    Specification<Budget> spec = Specification.where(BudgetSpecifications.belongsToUser(userId));
    
    // If date filtering is provided, combine it using 'and'
    if (filterStart != null && filterEnd != null) {
        spec = spec.and(BudgetSpecifications.activeBetween(filterStart, filterEnd));
    }
    
    return budgetRepository.findAll(spec);
}

    //saving budget
    public Budget saveBudget(Budget budget) {
        Validations.checkDate(budget.getStartDate());
        Validations.checkDate(budget.getEndDate());
        Validations.numberCheck(budget.getAmount(), "amount");
        Validations.checkObj(budget.getCategory(), "category");
        Validations.checkObj(budget.getUser(), "user");
        return budgetRepository.save(budget);
    }

    //deleting budget
    public boolean deleteBudgetForUser(Long budgetId, Long userId) {
        Optional<Budget> budgetOpt = budgetRepository.findById(budgetId);
        if (!budgetOpt.isPresent()) {
            // Optionally, you could throw a ResourceNotFoundException
            throw new RuntimeException("Budget not found");
        }
        Budget budget = budgetOpt.get();
    
        // Check if the budget belongs to the authenticated user
        if (!budget.getUser().getId().equals(userId)) {
            // If not, return false (or throw an exception)
            return false;
        }
    
        // Otherwise, delete the budget and return true
        budgetRepository.delete(budget);
        return true;
}


    //getting budget by id
    public Optional<Budget> getBudgetById(Long id) {                                                         
        return budgetRepository.findById(id);
    }
    
    //update amount
    @Transactional
    public Budget updateBudgetAmount(Long id, BigDecimal amount){
        Validations.numberCheck(amount, "Amount");
        Validations.negativeCheck(amount, "Amount");
        //get userId
        Long userId = SecurityUtil.getUserId();
        
        //get budget
        Budget budget = budgetRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Budget not found!"));
        
        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("Budget not found!");
        }
        
        budget.setAmount(amount);
        
        return budgetRepository.save(budget);
    }
    
    //get total spent on budget
    public BigDecimal getTotalSpentOnBudget(Long id){
        Optional<Budget> budgetOpt = budgetRepository.findById(id);
        if (budgetOpt.isEmpty()) {
            throw new RuntimeException("Budget not found!");
        }
        Budget budget = budgetOpt.get(); 
        
        //check ownership
        Long userId = SecurityUtil.getUserId();
        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("Budget not found!");
        }
        //get accounts for user
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Long> accountIds = new ArrayList<>();
        for(Account account : accounts){
            Long accId = account.getId();
            accountIds.add(accId);
        }
        
        Category category = budget.getCategory();
        return budgetRepository.getTotalSpentOnBudget(category.getId(), budget.getStartDate(), budget.getEndDate(), accountIds);
    } 
    
    // Recreate budgets for next month
    // (switch back to cron once you’re done testing)
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void createNextMonthBudgets() {
        // Compute date boundaries
        LocalDate today = LocalDate.now();
        LocalDate endOfCurrentMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate startOfNextMonth = endOfCurrentMonth.plusDays(1);
        LocalDate endOfNextMonth = startOfNextMonth.with(TemporalAdjusters.lastDayOfMonth());

        // Find all monthly budgets that are “active” right now
        List<Budget> currentBudgets = budgetRepository
            .findByMonthlyTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today);

        for (Budget current : currentBudgets) {
            User owner = current.getUser();  // use the user from the budget itself

            boolean exists = budgetRepository.existsByUserAndCategoryAndStartDateAndMonthlyTrue(
                owner,
                current.getCategory(),
                startOfNextMonth
        );

        if (!exists) {
            Budget next = new Budget(
                current.getAmount(),
                startOfNextMonth,
                endOfNextMonth,
                current.getCategory(),
                owner
            );
            budgetRepository.save(next);
        }
    }
}

    
    //set monthly active/inactive
    public Budget updateMonthlyStatus(Long id, boolean active){
        Long userId = SecurityUtil.getUserId();
        
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        
        if (!budget.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Budget not found");
        }
        
        budget.setMonthly(active);
        budgetRepository.save(budget);
        return budget;
    }
    
}
