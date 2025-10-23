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
import org.springframework.security.access.prepost.PreAuthorize;
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

    
    /**
     * Retrieves all budgets for the currently authenticated user, optionally filtered by date range.
     * <p>
     * Applies {@link JK.pfm.specification.BudgetSpecifications} to limit results
     * based on ownership and optional start/end date boundaries.
     *
     * @param filter the date range filter containing start and/or end dates
     * @return a list of matching {@link JK.pfm.model.Budget} entities
     */
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

    /**
     * Creates and saves a new budget for the currently authenticated user.
     *
     * @param request the budget creation payload containing amount, category ID, and date range
     * @return the created {@link JK.pfm.model.Budget}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the specified category does not exist (404 NOT FOUND)
     */
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
    /**
     * Deletes a budget by ID for the currently authenticated user.
     *
     * @param budgetId the ID of the budget to delete
     * @return {@code true} if deletion succeeds
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the budget is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserBudget(#budgetId)")
    public boolean deleteBudgetForUser(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Budget not found"
            ));
        budgetRepository.delete(budget);
        return true;
    }


    /**
     * Retrieves a budget by its ID.
     *
     * @param id the budget ID
     * @return an {@link java.util.Optional} containing the {@link JK.pfm.model.Budget} if found
     */
    public Optional<Budget> getBudgetById(Long id) {                                                         
        return budgetRepository.findById(id);
    }
    
    /**
     * Updates the amount of an existing budget.
     *
     * @param id the ID of the budget to update
     * @param request the payload containing the new budget amount
     * @return the updated {@link JK.pfm.model.Budget}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the budget is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserBudget(#id)")
    @Transactional
    public Budget updateBudgetAmount(Long id, UpdateBudgetAmountDto request){    
        //get budget
        Budget budget = budgetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Budget not found"
            ));
        
        budget.setAmount(request.getAmount());
        
        return budgetRepository.save(budget);
    }
    
    /**
     * Calculates the total amount spent within the category and date range of a budget.
     *
     * @param id the ID of the budget to evaluate
     * @return the total amount spent as {@link java.math.BigDecimal}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the budget is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserBudget(#id)")
    public BigDecimal getTotalSpentOnBudget(Long id){
        //get budget
        Budget budget = budgetRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Budget not found"
            ));

        //get accounts for user
        List<Long> accountIds = accountUtil.getUserAccountIds();
        
        return budgetRepository.getTotalSpentOnBudget(budget.getCategory().getId(), budget.getStartDate(), budget.getEndDate(), accountIds);
    } 
    
    /**
     * Automatically recreates monthly budgets for the next month.
     * <p>
     * Scheduled to run daily at midnight. For each active monthly budget,
     * creates a new instance for the upcoming month if it does not already exist.
     */
    @Scheduled(cron = "0 0 0 * * *")
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

    
    /**
     * Updates the monthly recurrence status of a budget.
     *
     * @param id the ID of the budget to update
     * @param active {@code true} to enable monthly recurrence, {@code false} to disable it
     * @return the updated {@link JK.pfm.model.Budget}
     * @throws org.springframework.web.server.ResponseStatusException
     *         if the budget is not found (404 NOT FOUND)
     */
    @PreAuthorize("@securityUtil.isCurrentUserBudget(#id)")
    public Budget updateMonthlyStatus(Long id, boolean active){
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));

        budget.setMonthly(active);
        budgetRepository.save(budget);
        return budget;
    }
    
}
