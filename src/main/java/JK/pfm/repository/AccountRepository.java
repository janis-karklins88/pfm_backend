
package JK.pfm.repository;



import JK.pfm.model.Account;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM Account a WHERE a.user.id = :userId")
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);

    
}

