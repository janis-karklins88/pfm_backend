
package JK.pfm.repository;



import JK.pfm.model.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

	/**
	 * Returns the total balance across all active accounts belonging to the specified user.
	 *
	 * <p>Accounts marked as inactive are excluded from the calculation.
	 * If no active accounts exist, returns {@code 0}.</p>
	 *
	 * @param userId the ID of the user
	 * @return total active balance as a {@link BigDecimal}
	 */
    @Query("""
      SELECT COALESCE(SUM(a.amount), 0)
      FROM Account a
      WHERE a.user.id = :userId
        AND a.active = true
    """)
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);

	/**
	 * Finds an active account by its name.
	 *
	 * @param name the account name
	 * @return an {@link Optional} containing the account if found and active
	 */
    Optional<Account> findByNameAndActiveTrue(String name);

	/**
	 * Finds an active account by user ID and account name.
	 *
	 * @param userId the owner's user ID
	 * @param name the account name
	 * @return an {@link Optional} containing the matching active account if found
	 */
    Optional<Account> findByUserIdAndNameAndActiveTrue(Long userId, String name);

	/**
	 * Finds an active account by user ID and account ID.
	 *
	 * @param userId the owner's user ID
	 * @param accountId the account's ID
	 * @return an {@link Optional} containing the matching active account if found
	 */
    Optional<Account> findByUserIdAndIdAndActiveTrue(Long userId, Long accountId);

	/**
	 * Retrieves all active accounts for the specified user.
	 *
	 * @param userId the owner's user ID
	 * @return list of active {@link Account} objects belonging to the user
	 */
    List<Account> findByUserIdAndActiveTrue(Long userId);
}


