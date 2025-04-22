package JK.pfm.repository;

import JK.pfm.model.SavingsGoal;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long>, JpaSpecificationExecutor<SavingsGoal> {
    // You can add custom query methods if needed.
    @Query("SELECT COALESCE(SUM(a.currentAmount), 0) FROM SavingsGoal a WHERE a.user.id = :userId")
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);
    
    
    
    //get last months savings balance
    @Query("""
    SELECT COALESCE(MAX(g.lastMonthAmount), 0)
    FROM SavingsGoal g
    WHERE g.user.id = :userId
    """)
    BigDecimal getLastMonthBalanceByUserId(@Param("userId") Long userId);

    
    /**
     * Find every user ID that has at least one SavingsGoal.
     * Used by the scheduler to know which users to snapshot.
     */
    @Query("SELECT DISTINCT g.user.id FROM SavingsGoal g")
    List<Long> findDistinctUserIds();
    
    /**
     * Bulk‐update the lastMonthAmount for all SavingsGoal rows of a given user.
     * Returns the number of rows updated.
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

    }
