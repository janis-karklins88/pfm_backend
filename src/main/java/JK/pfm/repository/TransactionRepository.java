package JK.pfm.repository;

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
    
    //query for suming total expense/income over time
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.account.id IN :accountIds AND t.date BETWEEN :start AND :end")
    BigDecimal sumByTypeAndDate(@Param("type") String type, 
                            @Param("accountIds") List<Long> accountIds, 
                            @Param("start") LocalDate start, 
                            @Param("end") LocalDate end);
    
    //query for getting expenses by category breakdown
    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = 'Expense' AND t.date BETWEEN :start AND :end GROUP BY t.category")
    List<Object[]> sumExpensesByCategory(@Param("start") LocalDate start,
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
    
    //find with paremeters
    
    
}
