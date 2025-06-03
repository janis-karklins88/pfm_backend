
package JK.pfm;

import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.UpdatePaymentAmountDto;
import JK.pfm.dto.UpdatePaymentNextDueDateDto;
import JK.pfm.dto.UpdateRecurringExpenseAccountDto;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.RecurringExpenseRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.RecurringExpenseService;
import JK.pfm.service.TransactionService;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SavingsGoalServiceTest {
    @Mock SavingsGoalRepository savingsGoalRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock TransactionService transactionService;
    @Mock AccountUtil accountUtil;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    
    private User user;
    private MockedStatic<SecurityUtil> securityUtilMock;
    
    @BeforeEach
    void setUp(){
        securityUtilMock = mockStatic(SecurityUtil.class);
        user = new User();
        user.setId(1L);
        securityUtilMock.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(user);
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(1L);
                
    }
    
    @AfterEach
    void tearDown(){
        securityUtilMock.close();
    }
    
    
}
