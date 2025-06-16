
package JK.pfm.Util;

import JK.pfm.model.Account;
import JK.pfm.model.Budget;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.SavingsGoal;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.util.SecurityUtil;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock TransactionRepository txnRepo;
    @Mock SavingsGoalRepository goalRepo;
    @Mock BudgetRepository budgetRepo;
    @Mock AccountRepository accountRepo;
    @Mock RecurringExpenseRepository autoPayRepo;
    @Mock UserRepository userRepo;

    SecurityUtil securityUtil;

    @BeforeEach
    void setup() {
        securityUtil = new SecurityUtil(txnRepo, goalRepo, budgetRepo, accountRepo, autoPayRepo);

        // Set fake authenticated user
        CustomUserDetails userDetails = new CustomUserDetails(123L, "user", "pass");
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getUserId_returnsCorrectId() {
        Long id = SecurityUtil.getUserId();
        assertEquals(123L, id);
    }

    @Test
    void getUser_returnsUserById() {
        User user = new User();
        user.setId(123L);

        when(userRepo.findById(123L)).thenReturn(Optional.of(user));

        User result = SecurityUtil.getUser(userRepo);
        assertEquals(123L, result.getId());
    }

    @Test
    void getUser_throwsIfNotFound() {
        when(userRepo.findById(123L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> SecurityUtil.getUser(userRepo));
    }

    @Test
void isCurrentUserTransaction_returnsTrueWhenOwner() {
    Transaction transaction = mock(Transaction.class);
    Account account = mock(Account.class);
    User user = new User(); user.setId(123L);

    when(transaction.getAccount()).thenReturn(account);
    when(account.getUser()).thenReturn(user);
    when(txnRepo.findById(1L)).thenReturn(Optional.of(transaction));

    assertTrue(securityUtil.isCurrentUserTransaction(1L));
}

@Test
void isCurrentUserTransaction_returnsFalseWhenNotOwner() {
    Transaction transaction = mock(Transaction.class);
    Account account = mock(Account.class);
    User user = new User(); user.setId(999L);

    when(transaction.getAccount()).thenReturn(account);
    when(account.getUser()).thenReturn(user);
    when(txnRepo.findById(1L)).thenReturn(Optional.of(transaction));

    assertFalse(securityUtil.isCurrentUserTransaction(1L));
}

@Test
void isCurrentUserTransaction_returnsFalseWhenNotFound() {
    when(txnRepo.findById(1L)).thenReturn(Optional.empty());
    assertFalse(securityUtil.isCurrentUserTransaction(1L));
}



@Test
void isCurrentUserSavingsGoal_returnsTrueWhenOwner() {
    SavingsGoal goal = mock(SavingsGoal.class);
    User user = new User(); user.setId(123L);

    when(goal.getUser()).thenReturn(user);
    when(goalRepo.findById(1L)).thenReturn(Optional.of(goal));

    assertTrue(securityUtil.isCurrentUserSavingsGoal(1L));
}

@Test
void isCurrentUserSavingsGoal_returnsFalseWhenNotOwner() {
    SavingsGoal goal = mock(SavingsGoal.class);
    User user = new User(); user.setId(999L);

    when(goal.getUser()).thenReturn(user);
    when(goalRepo.findById(1L)).thenReturn(Optional.of(goal));

    assertFalse(securityUtil.isCurrentUserSavingsGoal(1L));
}

@Test
void isCurrentUserSavingsGoal_returnsFalseWhenNotFound() {
    when(goalRepo.findById(1L)).thenReturn(Optional.empty());
    assertFalse(securityUtil.isCurrentUserSavingsGoal(1L));
}

@Test
void isCurrentUserBudget_returnsTrueWhenOwner() {
    Budget budget = mock(Budget.class);
    User user = new User(); user.setId(123L);

    when(budget.getUser()).thenReturn(user);
    when(budgetRepo.findById(1L)).thenReturn(Optional.of(budget));

    assertTrue(securityUtil.isCurrentUserBudget(1L));
}

@Test
void isCurrentUserBudget_returnsFalseWhenNotOwner() {
    Budget budget = mock(Budget.class);
    User user = new User(); user.setId(999L);

    when(budget.getUser()).thenReturn(user);
    when(budgetRepo.findById(1L)).thenReturn(Optional.of(budget));

    assertFalse(securityUtil.isCurrentUserBudget(1L));
}

@Test
void isCurrentUserBudget_returnsFalseWhenNotFound() {
    when(budgetRepo.findById(1L)).thenReturn(Optional.empty());
    assertFalse(securityUtil.isCurrentUserBudget(1L));
}

@Test
void isCurrentUserAccount_returnsTrueWhenOwner() {
    Account account = mock(Account.class);
    User user = new User(); user.setId(123L);

    when(account.getUser()).thenReturn(user);
    when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

    assertTrue(securityUtil.isCurrentUserAccount(1L));
}

@Test
void isCurrentUserAccount_returnsFalseWhenNotOwner() {
    Account account = mock(Account.class);
    User user = new User(); user.setId(999L);

    when(account.getUser()).thenReturn(user);
    when(accountRepo.findById(1L)).thenReturn(Optional.of(account));

    assertFalse(securityUtil.isCurrentUserAccount(1L));
}

@Test
void isCurrentUserAccount_returnsFalseWhenNotFound() {
    when(accountRepo.findById(1L)).thenReturn(Optional.empty());
    assertFalse(securityUtil.isCurrentUserAccount(1L));
}

@Test
void isCurrentUserAutoPay_returnsTrueWhenOwner() {
    RecurringExpense autoPay = mock(RecurringExpense.class);
    Account account = mock(Account.class);
    User user = new User(); user.setId(123L);

    when(autoPay.getAccount()).thenReturn(account);
    when(account.getUser()).thenReturn(user);
    when(autoPayRepo.findById(1L)).thenReturn(Optional.of(autoPay));

    assertTrue(securityUtil.isCurrentUserAutoPay(1L));
}

@Test
void isCurrentUserAutoPay_returnsFalseWhenNotOwner() {
    RecurringExpense autoPay = mock(RecurringExpense.class);
    Account account = mock(Account.class);
    User user = new User(); user.setId(999L);

    when(autoPay.getAccount()).thenReturn(account);
    when(account.getUser()).thenReturn(user);
    when(autoPayRepo.findById(1L)).thenReturn(Optional.of(autoPay));

    assertFalse(securityUtil.isCurrentUserAutoPay(1L));
}

@Test
void isCurrentUserAutoPay_returnsFalseWhenNotFound() {
    when(autoPayRepo.findById(1L)).thenReturn(Optional.empty());
    assertFalse(securityUtil.isCurrentUserAutoPay(1L));
}

}
