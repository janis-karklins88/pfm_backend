package JK.pfm.repository;

import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long>, JpaSpecificationExecutor<Budget> {
    //total spent on budget
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
       "WHERE t.category.id = :categoryId " +
       "AND t.date BETWEEN :startDate AND :endDate " +
       "AND t.account.id IN :accountIds")
    BigDecimal getTotalSpentOnBudget(
        @Param("categoryId") Long categoryId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("accountIds") List<Long> accountIds);

    
    // Retrieve budgets by category, although method is already provided, using like this because of possible updates.
    @Query("SELECT b FROM Budget b")
    List<Budget> findAllBudgets();
    
    //get current monthly budgets
    List<Budget> findByMonthlyTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date1, LocalDate date2);
    
    //check if budget exists for next month
    boolean existsByUserAndCategoryAndStartDateAndMonthlyTrue(User user, Category category, LocalDate startDate);
    

}
