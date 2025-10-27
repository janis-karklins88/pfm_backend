package JK.pfm.util;

import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility component that provides helper methods for account-related lookups,
 * typically used by services to resolve the current user’s accounts.
 *
 * <p>This class abstracts common account access logic and depends on
 * {@link AccountRepository} and {@link SecurityUtil} to ensure that lookups
 * are always scoped to the authenticated user.</p>
 *
 * <p>Intended for internal use within the service layer.</p>
 */
@Component
public class AccountUtil {

    private final AccountRepository accountRepository;

    public AccountUtil(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Retrieves the IDs of all active accounts belonging to the authenticated user.
     *
     * <p>The current user ID is resolved via {@link SecurityUtil#getUserId()}.
     * If the user has no accounts, an empty list is returned.</p>
     *
     * @return list of active account IDs owned by the current user (never {@code null})
     */
    public List<Long> getUserAccountIds() {
        Long userId = SecurityUtil.getUserId();
        return accountRepository
            .findByUserIdAndActiveTrue(userId)               // returns List<Account> or empty list
            .stream()
            .map(Account::getId)
            .collect(Collectors.toList());      // empty list if source is empty
    }
}
