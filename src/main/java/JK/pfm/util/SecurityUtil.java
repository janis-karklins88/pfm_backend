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


    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }


    public static User getUser(UserRepository userRepository) {
        Long userId = getUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
    
    public boolean isCurrentUserTransaction(Long txnId) {
        return txnRepo.findById(txnId)
        .map(t -> t.getAccount().getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    public boolean isCurrentUserSavingsGoal(Long goalId) {
        return goalRepo.findById(goalId)
        .map(g -> g.getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    public boolean isCurrentUserBudget(Long budgetId) {
        return budgetRepo.findById(budgetId)
        .map(b -> b.getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    public boolean isCurrentUserAccount(Long accountId) {
        return accountRepo.findById(accountId)
        .map(a -> a.getUser().getId().equals(getUserId()))
        .orElse(false);
    }

    public boolean isCurrentUserAutoPay(Long autoPayId) {
        return autoPayRepo.findById(autoPayId)
        .map(a -> a.getAccount().getUser().getId().equals(getUserId()))
        .orElse(false);
    }
}
