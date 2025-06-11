
package JK.pfm.repository;



import JK.pfm.model.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Only sum balances of active accounts */
    @Query("""
      SELECT COALESCE(SUM(a.amount), 0)
      FROM Account a
      WHERE a.user.id = :userId
        AND a.active = true
    """)
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);

    /** Lookup by name but only if the account is still active */
    Optional<Account> findByNameAndActiveTrue(String name);

    /** Scoped by user and name, only active ones */
    Optional<Account> findByUserIdAndNameAndActiveTrue(Long userId, String name);

    /** Scoped by user and id, only active ones */
    Optional<Account> findByUserIdAndIdAndActiveTrue(Long userId, Long accountId);

    /** All active accounts for this user */
    List<Account> findByUserIdAndActiveTrue(Long userId);
}


