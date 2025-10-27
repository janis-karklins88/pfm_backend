package JK.pfm.util;

import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Security utility providing helper methods for retrieving the authenticated user
 * and verifying resource ownership.
 *
 * <p>This class serves two main purposes:</p>
 * <ul>
 *   <li>Expose static methods ({@link #getUserId()}, {@link #getUser(UserRepository)}) for
 *       identifying the current authenticated user within services.</li>
 *   <li>Expose instance methods used in {@code @PreAuthorize} expressions to verify
 *       that a resource (transaction, account, budget, etc.) belongs to the current user.</li>
 * </ul>
 *
 * <p>Typical usage examples:</p>
 * <pre>
 * // In a service or controller:
 * Long userId = SecurityUtil.getUserId();
 *
 * // In a method-level security annotation:
 * @PreAuthorize("@securityUtil.isCurrentUserTransaction(#id)")
 * public void deleteTransaction(Long id) { ... }
 * </pre>
 *
 * <p><strong>Thread safety:</strong> The class is stateless except for its repository
 * dependencies and can be safely used as a singleton Spring bean.</p>
 */
public class SecurityUtil {
    
    private final TransactionRepository txnRepo;
    private final SavingsGoalRepository goalRepo;
    private final BudgetRepository budgetRepo;
    private final AccountRepository accountRepo;
    private final RecurringExpenseRepository autoPayRepo;

    public SecurityUtil(
        TransactionRepository txnRepo,
        SavingsGoalRepository goalRepo,
        BudgetRepository budgetRepo,
        AccountRepository accountRepo,
        RecurringExpenseRepository autoPayRepo
    ) {
        this.txnRepo       = txnRepo;
        this.goalRepo      = goalRepo;
        this.budgetRepo    = budgetRepo;
        this.accountRepo   = accountRepo;
        this.autoPayRepo   = autoPayRepo;
    }


    /**
     * Returns the ID of the currently authenticated user.
     *
     * <p>The ID is extracted from the {@link CustomUserDetails} stored in
     * the {@link SecurityContextHolder}.</p>
     *
     * @return the authenticated user's ID
     * @throws ClassCastException if the authentication principal is not {@link CustomUserDetails}
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }


    /**
     * Retrieves the full {@link User} entity of the authenticated user
     * from the provided {@link UserRepository}.
     *
     * @param userRepository repository used to fetch the user
     * @return the authenticated {@link User} entity
     * @throws UsernameNotFoundException if the user no longer exists in the database
     */
    public static User getUser(UserRepository userRepository) {
        Long userId = getUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
    
    /**
     * Checks if a transaction with the given ID belongs to the authenticated user.
     *
     * @param txnId ID of the transaction
     * @return {@code true} if the transaction belongs to the current user, otherwise {@code false}
     */
    public boolean isCurrentUserTransaction(Long txnId) {
        return txnRepo.findById(txnId)
        .map(t -> t.getAccount().getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    /**
     * Checks if a savings goal with the given ID belongs to the authenticated user.
     *
     * @param goalId ID of the savings goal
     * @return {@code true} if the savings goal belongs to the current user, otherwise {@code false}
     */
    public boolean isCurrentUserSavingsGoal(Long goalId) {
        return goalRepo.findById(goalId)
        .map(g -> g.getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    /**
     * Checks if a budget with the given ID belongs to the authenticated user.
     *
     * @param budgetId ID of the budget
     * @return {@code true} if the budget belongs to the current user, otherwise {@code false}
     */
    public boolean isCurrentUserBudget(Long budgetId) {
        return budgetRepo.findById(budgetId)
        .map(b -> b.getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    /**
     * Checks if an account with the given ID belongs to the authenticated user.
     *
     * @param accountId ID of the account
     * @return {@code true} if the account belongs to the current user, otherwise {@code false}
     */
    public boolean isCurrentUserAccount(Long accountId) {
        return accountRepo.findById(accountId)
        .map(a -> a.getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    /**
     * Checks if a recurring expense with the given ID belongs to the authenticated user.
     *
     * @param autoPayId ID of the recurring expense
     * @return {@code true} if the recurring expense belongs to the current user, otherwise {@code false}
     */
    public boolean isCurrentUserAutoPay(Long autoPayId) {
        return autoPayRepo.findById(autoPayId)
        .map(a -> a.getAccount().getUser().getId().equals(getUserId()))
        .orElse(false);
    }
}
