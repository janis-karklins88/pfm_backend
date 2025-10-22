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
    
	/**
	 * Finds all transactions by category ID.
	 *
	 * @param categoryId the category identifier
	 * @return list of {@link Transaction} entries for the given category
	 */
    List<Transaction> findByCategoryId(Long categoryId);

	/**
	 * Retrieves the five most recent transactions for the specified accounts.
	 *
	 * <p>Orders by {@code date DESC, id DESC} to ensure stable ordering when dates are equal.</p>
	 *
	 * @param accountIds list of account IDs to include
	 * @return up to five most recent {@link Transaction} rows
	 */    
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

	/**
	 * Sums transaction amounts for a given type and period, limited to the provided accounts.
	 *
	 * <p>Excludes system categories (Savings, Opening Balance, Fund Transfer) and internal
	 * savings transfer descriptions ({@code "Deposit to savings"}, {@code "Withdraw from savings"}).
	 * Returns {@code 0} when no rows match.</p>
	 *
	 * @param type the transaction type (e.g., {@code "Expense"} or {@code "Deposit"})
	 * @param accountIds list of account IDs to include
	 * @param start inclusive start date
	 * @param end inclusive end date
	 * @return sum as {@link BigDecimal}, or {@code 0} if none
	 */
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

	/**
	 * Sums transaction amounts for a given type/category and period, limited to the provided accounts.
	 *
	 * <p>Excludes system categories and internal savings transfer descriptions.
	 * Returns {@code 0} when no rows match.</p>
	 *
	 * @param type the transaction type (e.g., {@code "Expense"} or {@code "Deposit"})
	 * @param accountIds list of account IDs to include
	 * @param start inclusive start date
	 * @param end inclusive end date
	 * @param categoryId the target category ID
	 * @return sum as {@link BigDecimal}, or {@code 0} if none
	 */
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

	/**
	 * Aggregates expense totals by category for the specified period and accounts.
	 *
	 * <p>Excludes system categories. Returns category name with its total expense.</p>
	 *
	 * @param accountIds list of account IDs to include
	 * @param start inclusive start date
	 * @param end inclusive end date
	 * @return list of {@link ExpenseByCategoryDTO} rows
	 */
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
	 * Aggregates expense totals by account for the specified period and accounts.
	 *
	 * <p>Excludes system categories. Returns account name with its total expense.</p>
	 *
	 * @param accountIds list of account IDs to include
	 * @param start inclusive start date
	 * @param end inclusive end date
	 * @return list of {@link ExpenseByAccountDTO} rows
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


	/**
	 * Returns daily totals grouped by date and type for the specified period.
	 *
	 * <p>Each element is an {@code Object[]} with the following layout:
	 * {@code [LocalDate date, String type, BigDecimal sum]}. Results are ordered by date ascending.</p>
	 *
	 * @param start inclusive start date
	 * @param end inclusive end date
	 * @return list of rows as {@code Object[]} triples (date, type, sum)
	 */
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
    
	/**
	 * Retrieves all transactions whose date falls within the given inclusive range.
	 *
	 * @param startDate inclusive start date
	 * @param endDate inclusive end date
	 * @return list of {@link Transaction} entries
	 */
    List<Transaction> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
	/**
	 * Checks whether any transaction exists for the specified accounts in the given date range.
	 *
	 * @param accountIds list of account IDs to check
	 * @param start inclusive start date
	 * @param end inclusive end date
	 * @return {@code true} if at least one transaction exists, otherwise {@code false}
	 */
    boolean existsByAccountIdInAndDateBetween(
        List<Long> accountIds,
        LocalDate start,
        LocalDate end
    );

	/**
	 * Computes the net savings movement for the specified period and accounts.
	 *
	 * <p>Only considers transactions in the {@code 'Fund Transfer'} category, and
	 * interprets {@code 'Deposit to savings'} as +amount and {@code 'Withdraw from savings'} as -amount.</p>
	 *
	 * @param accountIds list of account IDs to include
	 * @param start inclusive start date
	 * @param end inclusive end date
	 * @return net savings movement as {@link BigDecimal} (can be negative)
	 */
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
        AND LOWER(t.category.name) = 'fund transfer'
        AND t.date BETWEEN :start AND :end
    """)
    BigDecimal netSavingsMonthlyBalance(
        @Param("accountIds") List<Long> accountIds,
        @Param("start")      LocalDate start,
        @Param("end")        LocalDate end
    );

	/**
	 * Returns cumulative savings balance up to a cut-off date for a user.
	 *
	 * <p>Only considers {@code 'Fund Transfer'} category rows and interprets savings
	 * deposits/withdrawals via transaction description, producing a net historical total.</p>
	 *
	 * @param userId the user's ID
	 * @param cutoffDate inclusive cut-off date
	 * @return cumulative savings balance as of {@code cutoffDate}
	 */
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
        AND t.category.name      = 'Fund Transfer'
        AND t.date              <= :cutoffDate
    """)
    BigDecimal getSavingsBalanceUpTo(
        @Param("userId")     Long userId,
        @Param("cutoffDate") LocalDate cutoffDate
    );

	/**
	 * Computes cumulative account balance up to a cut-off date, excluding system categories.
	 *
	 * <p>Interprets {@code Deposit} as +amount and {@code Expense} as -amount.
	 * Excludes Savings, Opening Balance, and Fund Transfer categories.</p>
	 *
	 * @param userId the user's ID
	 * @param cutoffDate inclusive cut-off date
	 * @return cumulative balance as of {@code cutoffDate}
	 */
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
