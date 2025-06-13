package JK.pfm.repository;

import JK.pfm.dto.ExpenseByAccountDTO;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
    extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByCategoryId(Long categoryId);

    @Query(
    value = """
      SELECT *
        FROM transactions
       WHERE account_id IN (:accountIds)
       ORDER BY date DESC, id DESC
       LIMIT 5
    """,
    nativeQuery = true
  )
    List<Transaction> findTop5ByAccountIdInOrderByIdDesc(
            @Param("accountIds") List<Long> accountIds
    );

    // total expense and income, ignoring Savings, Opening Balance, Fund Transfer
    @Query("""
      SELECT COALESCE(SUM(t.amount), 0)
      FROM Transaction t
      WHERE t.type            = :type
        AND t.account.id      IN :accountIds
        AND t.date            BETWEEN :start AND :end
        AND LOWER(t.category.name) NOT IN (
            'savings', 'opening balance', 'fund transfer'
        )
        AND t.description NOT IN (
            'Deposit to savings', 'Withdraw from savings'
        )
    """)
    BigDecimal sumByTypeAndDate(
        @Param("type")       String type,
        @Param("accountIds") List<Long> accountIds,
        @Param("start")      LocalDate start,
        @Param("end")        LocalDate end
    );

    // total category expense for a specific user category, still ignoring system cats
    @Query("""
      SELECT COALESCE(SUM(t.amount), 0)
      FROM Transaction t
      WHERE t.type            = :type
        AND t.account.id      IN :accountIds
        AND t.date            BETWEEN :start AND :end
        AND LOWER(t.category.name) NOT IN (
            'savings', 'opening balance', 'fund transfer'
        )
        AND t.category.id     = :categoryId
        AND t.description NOT IN (
            'Deposit to savings', 'Withdraw from savings'
        )
    """)
    BigDecimal sumByTypeAndDateAndCategory(
        @Param("type")       String type,
        @Param("accountIds") List<Long> accountIds,
        @Param("start")      LocalDate start,
        @Param("end")        LocalDate end,
        @Param("categoryId") Long categoryId
    );

    // expense-by-category breakdown, excluding system categories
    @Query("""
      SELECT new JK.pfm.dto.ExpenseByCategoryDTO(
        t.category.name,
        COALESCE(SUM(t.amount), 0)
      )
      FROM Transaction t
      WHERE t.type = 'Expense'
        AND t.account.id IN :accountIds
        AND LOWER(t.category.name) NOT IN (
            'savings', 'opening balance', 'fund transfer'
        )
        AND t.date BETWEEN :start AND :end
      GROUP BY t.category.name
    """)
    List<ExpenseByCategoryDTO> findExpensesByCategory(
        @Param("accountIds") List<Long> accountIds,
        @Param("start")      LocalDate start,
        @Param("end")        LocalDate end
    );
    
     /**
     * Net “Expense” per account over the given period,
     * excluding system categories (Savings, Opening Balance, Fund Transfer).
     */
    @Query("""
      SELECT new JK.pfm.dto.ExpenseByAccountDTO(
        t.account.name,
        COALESCE(SUM(t.amount), 0)
      )
      FROM Transaction t
      WHERE t.type = 'Expense'
        AND t.account.id IN :accountIds
        AND LOWER(t.category.name) NOT IN (
            'savings', 'opening balance', 'fund transfer'
        )
        AND t.date BETWEEN :start AND :end
      GROUP BY t.account.name
    """)
    List<ExpenseByAccountDTO> findExpensesByAccount(
        @Param("accountIds") List<Long> accountIds,
        @Param("start")      LocalDate start,
        @Param("end")        LocalDate end
    );


    // daily trends (no change needed here)
    @Query("""
      SELECT t.date, t.type, COALESCE(SUM(t.amount), 0)
      FROM Transaction t
      WHERE t.date BETWEEN :start AND :end
      GROUP BY t.date, t.type
      ORDER BY t.date
    """)
    List<Object[]> getDailyTrends(
        @Param("start") LocalDate start,
        @Param("end")   LocalDate end
    );

    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByAccountIdInAndDateBetween(
        List<Long> accountIds,
        LocalDate start,
        LocalDate end
    );

    // net savings balance (unchanged)
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
      WHERE t.account.id IN :accountIds
        AND LOWER(t.category.name) = 'savings'
        AND t.date BETWEEN :start AND :end
    """)
    BigDecimal netSavingsMonthlyBalance(
        @Param("accountIds") List<Long> accountIds,
        @Param("start")      LocalDate start,
        @Param("end")        LocalDate end
    );

    // get cumulative savings up to cutoff (unchanged)
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
      WHERE t.account.user.id   = :userId
        AND t.category.name      = 'Savings'
        AND t.date              <= :cutoffDate
    """)
    BigDecimal getSavingsBalanceUpTo(
        @Param("userId")     Long userId,
        @Param("cutoffDate") LocalDate cutoffDate
    );

    // cumulative account balance, ignoring system categories
    @Query("""
      SELECT COALESCE(
        SUM(
          CASE
            WHEN t.type = 'Deposit' THEN t.amount
            WHEN t.type = 'Expense' THEN -t.amount
            ELSE 0
          END
        ), 0)
      FROM Transaction t
      WHERE t.account.user.id         = :userId
        AND LOWER(t.category.name) NOT IN (
            'savings', 'opening balance', 'fund transfer'
        )
        AND t.date                   <= :cutoffDate
    """)
    BigDecimal getAccountBalanceUpTo(
        @Param("userId")     Long userId,
        @Param("cutoffDate") LocalDate cutoffDate
    );
}
