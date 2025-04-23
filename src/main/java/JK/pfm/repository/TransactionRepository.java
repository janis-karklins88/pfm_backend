package JK.pfm.repository;

import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    
    List<Transaction> findByCategoryId(Long categoryId);
    
    List<Transaction> findTop5ByAccountIdInOrderByIdDesc(List<Long> accountIds);

    //total expense and income, ignoring transactions for savings
    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
       "FROM Transaction t " +
       "WHERE t.type = :type " +
       "AND t.account.id IN :accountIds " +
       "AND t.date BETWEEN :start AND :end " +
       "AND t.description NOT IN ('Deposit to savings', 'Withdraw from savings')")
    BigDecimal sumByTypeAndDate(@Param("type") String type, 
                            @Param("accountIds") List<Long> accountIds, 
                            @Param("start") LocalDate start, 
                            @Param("end") LocalDate end);
    
    //total category expense for last 10 + current month
    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
       "FROM Transaction t " +
       "WHERE t.type = :type " +
       "AND t.account.id IN :accountIds " +
       "AND t.date BETWEEN :start AND :end " +
       "AND t.category.id = :categoryId " +
       "AND t.description NOT IN ('Deposit to savings', 'Withdraw from savings')")
    BigDecimal sumByTypeAndDateAndCategory(@Param("type") String type, 
                                       @Param("accountIds") List<Long> accountIds, 
                                       @Param("start") LocalDate start, 
                                       @Param("end") LocalDate end,
                                       @Param("categoryId") Long categoryId);


    
    //query for getting expenses by category breakdown
    @Query("SELECT new JK.pfm.dto.ExpenseByCategoryDTO(t.category.name, COALESCE(SUM(t.amount), 0)) " +
       "FROM Transaction t " +
       "WHERE t.type = 'Expense' " +
       "AND t.account.id IN :accountIds " +
       "AND t.category.name <> 'Savings'" +
       "AND t.date BETWEEN :start AND :end " +
       "GROUP BY t.category.name")
    List<ExpenseByCategoryDTO> findExpensesByCategory(@Param("accountIds") List<Long> accountIds,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    
    //query for grouping transaction by date and type over time, for making trends
    @Query("SELECT t.date, t.type, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t " +
           "WHERE t.date BETWEEN :start AND :end " +
           "GROUP BY t.date, t.type " +
           "ORDER BY t.date")
    List<Object[]> getDailyTrends(@Param("start") LocalDate start,
                                  @Param("end") LocalDate end);
    
    //finding transactions between dates
    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    //check if date range has any transactions
    boolean existsByAccountIdInAndDateBetween(List<Long> accountIds, LocalDate start, LocalDate end);
    
    //get net savings balance for dates
    @Query("""
      SELECT COALESCE(
        SUM(
          CASE
            WHEN t.description = 'Deposit to savings'  THEN t.amount
            WHEN t.description = 'Withdraw from savings' THEN -t.amount
            ELSE 0
          END
        ), 0)
      FROM Transaction t
      WHERE 
        t.account.id IN :accountIds
        AND LOWER(t.category.name) = 'savings'
        AND t.date BETWEEN :start AND :end
    """)
    BigDecimal netSavingsMonthlyBalance(
        @Param("accountIds") List<Long> accountIds,
        @Param("start")      LocalDate start,
        @Param("end")        LocalDate end
    );
    
    //get last month saving goal balances
    // in your repository
    @Query("""
      SELECT COALESCE(
        SUM(
          CASE
            WHEN t.description = 'Deposit to savings'  THEN t.amount
            WHEN t.description = 'Withdraw from savings' THEN -t.amount
            ELSE 0
          END
        ), 0)
      FROM Transaction t
      WHERE t.account.user.id = :userId
        AND t.category.name    = 'Savings'
        AND t.date            <= :cutoffDate
    """)
    BigDecimal getSavingsBalanceUpTo(
      @Param("userId")     Long userId,
      @Param("cutoffDate") LocalDate cutoffDate
    );
    
    //get last month total blance
    /**
     * Cumulative account balance (summing deposits minus withdrawals)
     * for all accounts of a user up to and including the given cutoff date,
     * but ignoring any transactions in the “Savings” category.
     */
    @Query("""
      SELECT COALESCE(
        SUM(
          CASE
            WHEN t.type = 'Deposit'    THEN t.amount
            WHEN t.type = 'Expense' THEN -t.amount
            ELSE 0
          END
        ), 0)
      FROM Transaction t
      WHERE t.account.user.id        = :userId
        AND LOWER(t.category.name)  <> 'savings'
        AND t.date                 <= :cutoffDate
    """)
    BigDecimal getAccountBalanceUpTo(
      @Param("userId")     Long userId,
      @Param("cutoffDate") LocalDate cutoffDate
    );
    
    
}
