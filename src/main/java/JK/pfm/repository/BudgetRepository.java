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
	/**
	 * Calculates the total amount spent for a given budget category and period.
	 *
	 * <p>Includes all transactions within the specified date range and limited
	 * to the provided list of account IDs. If no matching transactions exist,
	 * returns {@code 0}.</p>
	 *
	 * @param categoryId the ID of the budget's category
	 * @param startDate the start date of the budget period (inclusive)
	 * @param endDate the end date of the budget period (inclusive)
	 * @param accountIds list of account IDs associated with the budget
	 * @return total amount spent as a {@link BigDecimal}
	 */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
       "WHERE t.category.id = :categoryId " +
       "AND t.date BETWEEN :startDate AND :endDate " +
       "AND t.account.id IN :accountIds")
    BigDecimal getTotalSpentOnBudget(
        @Param("categoryId") Long categoryId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate, 
        @Param("accountIds") List<Long> accountIds);

    
	/**
	 * Retrieves all budget records.
	 *
	 * <p>Defined explicitly to allow custom query extensions or
	 * additional filtering in future updates.</p>
	 *
	 * @return list of all {@link Budget} entities
	 */
    @Query("SELECT b FROM Budget b")
    List<Budget> findAllBudgets();
    
	/**
	 * Finds all active monthly budgets covering the specified date.
	 *
	 * <p>A budget qualifies if its {@code monthly} flag is true and the
	 * provided date falls within its start and end dates.</p>
	 *
	 * @param date1 date used as the upper comparison bound (end date ≥ date1)
	 * @param date2 date used as the lower comparison bound (start date ≤ date2)
	 * @return list of matching monthly {@link Budget} entries
	 */
    List<Budget> findByMonthlyTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date1, LocalDate date2);
    
	/**
	 * Checks if a monthly budget already exists for a user, category,
	 * and start date combination.
	 *
	 * <p>Useful to prevent duplicate monthly budgets for the same period.</p>
	 *
	 * @param user the {@link User} owning the budget
	 * @param category the {@link Category} associated with the budget
	 * @param startDate the first day of the target month
	 * @return {@code true} if such a budget already exists, {@code false} otherwise
	 */
    boolean existsByUserAndCategoryAndStartDateAndMonthlyTrue(User user, Category category, LocalDate startDate);
    

}
