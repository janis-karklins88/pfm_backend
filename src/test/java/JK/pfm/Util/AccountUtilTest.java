
package JK.pfm.Util;

import JK.pfm.model.Account;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountUtilTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountUtil accountUtil;

    @BeforeEach
    void setUp() {
        accountUtil = new AccountUtil(accountRepository);
    }

    @Test
    void getUserAccountIds_returnsCorrectIds() {
        Long userId = 123L;

        // Prepare fake accounts
        Account a1 = new Account("acc1", BigDecimal.TEN, new User()); a1.setId(1L);
        Account a2 = new Account("acc2", BigDecimal.TEN, new User()); a2.setId(2L);

        // Mock static method
        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getUserId).thenReturn(userId);

            when(accountRepository.findByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(a1, a2));

            List<Long> ids = accountUtil.getUserAccountIds();

            assertEquals(List.of(1L, 2L), ids);
        }
    }

    @Test
    void getUserAccountIds_returnsEmptyListIfNoAccounts() {
        Long userId = 123L;

        try (MockedStatic<SecurityUtil> mocked = Mockito.mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getUserId).thenReturn(userId);

            when(accountRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

            List<Long> ids = accountUtil.getUserAccountIds();

            assertTrue(ids.isEmpty());
        }
    }
}

