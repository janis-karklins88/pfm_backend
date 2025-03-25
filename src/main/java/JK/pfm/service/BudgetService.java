package JK.pfm.service;

import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.specifications.BudgetSpecifications;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;
    
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
        Validations.checkObj(budget.getCategory(), "class");
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        
        //get budget
        Optional<Budget> budgetOpt = budgetRepository.findById(id);
        if (budgetOpt.isEmpty()) {
            throw new RuntimeException("Budget not found!");
        }
        Budget budget = budgetOpt.get();
        
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("Budget not found!");
        }
        
          
        Category category = budget.getCategory();
        return budgetRepository.getTotalSpentOnBudget(category.getId(), budget.getStartDate(), budget.getEndDate());
    } 
    
}
