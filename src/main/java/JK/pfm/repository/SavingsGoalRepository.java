package JK.pfm.repository;

import JK.pfm.model.SavingsGoal;
import JK.pfm.model.User;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long>, JpaSpecificationExecutor<SavingsGoal> {

    	/**
	 * Calculates the total savings balance for a specific user.
	 *
	 * <p>Sums all {@code currentAmount} values across the user's {@link SavingsGoal} records.
	 * If the user has no savings goals, returns {@code 0}.</p>
	 *
	 * @param userId the ID of the user
	 * @return total current balance as a {@link BigDecimal}
	 */
    @Query("SELECT COALESCE(SUM(a.currentAmount), 0) FROM SavingsGoal a WHERE a.user.id = :userId")
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);
    
    
    
	/**
	 * Retrieves the last recorded total savings balance for a specific user.
	 *
	 * <p>Returns the most recent {@code lastMonthAmount} value among the user's savings goals.
	 * If no record exists, returns {@code 0}.</p>
	 *
	 * @param userId the ID of the user
	 * @return last month's recorded savings balance as a {@link BigDecimal}
	 */
    @Query("""
    SELECT COALESCE(MAX(g.lastMonthAmount), 0)
    FROM SavingsGoal g
    WHERE g.user.id = :userId
    """)
    BigDecimal getLastMonthBalanceByUserId(@Param("userId") Long userId);

    
	/**
	 * Retrieves all unique user IDs that currently have at least one savings goal.
	 *
	 * <p>Used by background schedulers to determine which users should be included
	 * in monthly snapshot operations.</p>
	 *
	 * @return list of distinct user IDs owning at least one {@link SavingsGoal}
	 */
    @Query("SELECT DISTINCT g.user.id FROM SavingsGoal g")
    List<Long> findDistinctUserIds();
    
	/**
	 * Updates the {@code lastMonthAmount} field for all savings goals belonging to a given user.
	 *
	 * <p>Used during scheduled month-end processing to record the user's last known balance.
	 * Returns the total number of rows updated.</p>
	 *
	 * @param userId the ID of the user whose savings goals should be updated
	 * @param amount the amount to set as {@code lastMonthAmount}
	 * @return number of updated records
	 */
    @Modifying
    @Transactional
    @Query("""
      UPDATE SavingsGoal g
      SET g.lastMonthAmount = :amount
      WHERE g.user.id = :userId
    """)
    int updateLastMonthAmountByUserId(
        @Param("userId") Long userId,
        @Param("amount") BigDecimal amount
    );
    
    boolean existsByUserAndNameIgnoreCase(User user, String name);

    }
