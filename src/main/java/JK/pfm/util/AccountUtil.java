package JK.pfm.util;

import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountUtil {

    private final AccountRepository accountRepository;

    public AccountUtil(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Fetches the IDs of all accounts belonging to the current user.
     * If the user has no accounts, returns an empty list.
     */
    public List<Long> getUserAccountIds() {
        Long userId = SecurityUtil.getUserId();
        return accountRepository
            .findByUserId(userId)               // returns List<Account> or empty list
            .stream()
            .map(Account::getId)
            .collect(Collectors.toList());      // empty list if source is empty
    }
}
