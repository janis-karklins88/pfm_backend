package JK.pfm.service;

import JK.pfm.dto.BudgetCreationRequest;
import JK.pfm.dto.UpdateBudgetAmountDto;
import JK.pfm.dto.filters.DateRangeFilter;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.specifications.BudgetSpecifications;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;


@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountUtil accountUtil;

    public BudgetService(
            BudgetRepository budgetRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            AccountUtil accountUtil
    ) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.accountUtil = accountUtil;
    }

    
    //getting all budgets for user
    public List<Budget> getAllBudgets(DateRangeFilter filter) {
    // Start with the specification that filters budgets by user
    Specification<Budget> spec = Specification.where(BudgetSpecifications.belongsToUser(SecurityUtil.getUserId()));
    
    // If date filtering is provided, combine it using 'and'
    if (filter.getStartDate() != null && filter.getEndDate() != null) {
        spec = spec.and(BudgetSpecifications.activeBetween(filter.getStartDate(), filter.getEndDate()));
    }
    else if (filter.getStartDate() != null) {
        spec = spec.and(BudgetSpecifications.startDateOnOrAfter(filter.getStartDate()));
    }
    else if (filter.getEndDate() != null) {
        spec = spec.and(BudgetSpecifications.endDateOnOrBefore(filter.getEndDate()));
    }

    return budgetRepository.findAll(spec);
}

    //saving budget
    public Budget saveBudget(BudgetCreationRequest request) {
        //get user
        User user = SecurityUtil.getUser(userRepository);
        
        //get category
        Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category not found"
            ));
        
        //create budget
        Budget budget = new Budget(request.getAmount(), request.getStartDate(), request.getEndDate(), category, user);
        
        return budgetRepository.save(budget);
    }

    //deleting budget
    public boolean deleteBudgetForUser(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Budget not found"
            ));
        
        Long userId = SecurityUtil.getUserId();
        // Check if the budget belongs to the authenticated user
        if (!budget.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Budget not found"
            );
        }

        budgetRepository.delete(budget);
        return true;
}


    //getting budget by id
    public Optional<Budget> getBudgetById(Long id) {                                                         
        return budgetRepository.findById(id);
    }
    
    //update amount
    @Transactional
    public Budget updateBudgetAmount(Long id, UpdateBudgetAmountDto request){    
        //get budget
        Budget budget = budgetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Budget not found"
            ));
        
        Long userId = SecurityUtil.getUserId();
        if (!budget.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Budget not found"
            );
        }
        
        budget.setAmount(request.getAmount());
        
        return budgetRepository.save(budget);
    }
    
    //get total spent on budget
    public BigDecimal getTotalSpentOnBudget(Long id){
        //get budget
        Budget budget = budgetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Budget not found"
            ));
        
        Long userId = SecurityUtil.getUserId();
        if (!budget.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Budget not found"
            );
        }
        
        //get accounts for user
        List<Long> accountIds = accountUtil.getUserAccountIds();
        
        return budgetRepository.getTotalSpentOnBudget(budget.getCategory().getId(), budget.getStartDate(), budget.getEndDate(), accountIds);
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
            next.setMonthly(true);
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
