
package JK.pfm.service;

import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.dto.UpdateSavingsAmountDto;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.SavingsGoal;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.SavingsGoalService;
import JK.pfm.service.TransactionService;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    
    @InjectMocks
    SavingsGoalService savingsGoalService;
    
    private User user;
    private MockedStatic<SecurityUtil> securityUtilMock;
    private SavingsGoal goal;
    
    @BeforeEach
    void setUp(){
        securityUtilMock = mockStatic(SecurityUtil.class);
        user = new User();
        user.setId(1L);
        securityUtilMock.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(user);
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(1L);
        
        goal = new SavingsGoal("test-goal", new BigDecimal("500"), "new car", user);
        goal.setId(10L);
        
        
                
    }
    
    @AfterEach
    void tearDown(){
        securityUtilMock.close();
    }
    
    @Test
    void saveSavingsGoal_nameExists_throwsConflict() {
    // Arrange
    SavingGoalCreation request = new SavingGoalCreation();
    request.setName("test-goal");
    request.setTargetAmount(new BigDecimal("500"));
    request.setDescription("new car");

    // Stub existsByUserAndNameIgnoreCase(user, "test-goal") → true
    when(savingsGoalRepository.existsByUserAndNameIgnoreCase(user, "test-goal"))
        .thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> savingsGoalService.saveSavingsGoal(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(rse.getReason())
                .isEqualTo("Savings goal with this name already exists");
        });

    verify(savingsGoalRepository).existsByUserAndNameIgnoreCase(user, "test-goal");
    verify(savingsGoalRepository, never()).save(any());
}

    @Test
    void saveSavingsGoal_happyPath_savesAndReturnsGoal() {
    // Arrange
    SavingGoalCreation request = new SavingGoalCreation();
    request.setName("test-goal");
    request.setTargetAmount(new BigDecimal("500"));
    request.setDescription("new car");

    // existsBy… → false
    when(savingsGoalRepository.existsByUserAndNameIgnoreCase(user, "test-goal"))
        .thenReturn(false);

    // Stub save(...) to set the ID and return the goal
    when(savingsGoalRepository.save(any(SavingsGoal.class)))
        .thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

    // Act
    SavingsGoal saved = savingsGoalService.saveSavingsGoal(request);

    // Assert: verify returned object has correct fields
    assertThat(saved.getId()).isEqualTo(10L);
    assertThat(saved.getName()).isEqualTo("test-goal");
    assertThat(saved.getTargetAmount()).isEqualByComparingTo("500");
    assertThat(saved.getDescription()).isEqualTo("new car");
    assertThat(saved.getUser()).isSameAs(user);
    assertThat(saved.getCurrentAmount()).isEqualByComparingTo("0"); // default

    // Verify interactions
    verify(savingsGoalRepository).existsByUserAndNameIgnoreCase(user, "test-goal");
    ArgumentCaptor<SavingsGoal> captor = ArgumentCaptor.forClass(SavingsGoal.class);
    verify(savingsGoalRepository).save(captor.capture());
    SavingsGoal toSave = captor.getValue();

    assertThat(toSave.getId()).isEqualTo(10L);
    assertThat(toSave.getName()).isEqualTo("test-goal");
    assertThat(toSave.getTargetAmount()).isEqualByComparingTo("500");
    assertThat(toSave.getDescription()).isEqualTo("new car");
    assertThat(toSave.getUser()).isSameAs(user);
    assertThat(toSave.getCurrentAmount()).isEqualByComparingTo("0");
    }
    
    @Test
    void deleteSavingsGoal_notFound_throws404() {
    // Arrange: stub findById(999L) → empty
    when(savingsGoalRepository.findById(999L))
        .thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> savingsGoalService.deleteSavingsGoal(999L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Savings goal is missing");
        });

    verify(savingsGoalRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteSavingsGoal_nonZeroBalance_throwsConflict() {
    // Arrange: stub findById(10L) → our shared goal, but give it a non-zero current amount
    goal.setCurrentAmount(new BigDecimal("100"));
    when(savingsGoalRepository.findById(10L))
        .thenReturn(Optional.of(goal));

    // Act & Assert
    assertThatThrownBy(() -> savingsGoalService.deleteSavingsGoal(10L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(rse.getReason())
                .isEqualTo("Savings goal still has funds. Please withdraw funds before deletiont");
        });

    verify(savingsGoalRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteSavingsGoal_happyPath_deletesById() {
    // Arrange: stub findById(10L) → shared goal with default currentAmount = 0
    goal.setCurrentAmount(BigDecimal.ZERO);
    when(savingsGoalRepository.findById(10L))
        .thenReturn(Optional.of(goal));

    // Act
    savingsGoalService.deleteSavingsGoal(10L);

    // Assert: deleteById should be called
    verify(savingsGoalRepository).deleteById(10L);
    }
    
    @Test
    void updateSavingsGoalAmount_notFound_throws404() {
    // Arrange: stub findById(999L) → empty
    when(savingsGoalRepository.findById(999L))
        .thenReturn(Optional.empty());

    UpdateSavingsAmountDto request = new UpdateSavingsAmountDto(new BigDecimal("750"));

    // Act & Assert
    assertThatThrownBy(() -> savingsGoalService.updateSavingsGoalAmount(999L, request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Savings goal is missing");
        });

    verify(savingsGoalRepository, never()).save(any());
    }

    @Test
    void updateSavingsGoalAmount_happyPath_savesNewTarget() {
    // Arrange: shared goal has id=10, default currentAmount=0
    when(savingsGoalRepository.findById(10L))
        .thenReturn(Optional.of(goal));
    when(savingsGoalRepository.save(any(SavingsGoal.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    UpdateSavingsAmountDto request = new UpdateSavingsAmountDto(new BigDecimal("750"));

    // Act
    SavingsGoal updated = savingsGoalService.updateSavingsGoalAmount(10L, request);

    // Assert: returned object has updated targetAmount
    assertThat(updated.getTargetAmount()).isEqualByComparingTo("750");

    // Verify save(...) was called with the same instance, updated
    ArgumentCaptor<SavingsGoal> captor = ArgumentCaptor.forClass(SavingsGoal.class);
    verify(savingsGoalRepository).save(captor.capture());
    SavingsGoal toSave = captor.getValue();

    assertThat(toSave.getId()).isEqualTo(10L);
    assertThat(toSave.getTargetAmount()).isEqualByComparingTo("750");
    }
    
    @Test
    void getNetMonthlyBalance_noAccounts_returnsEmptyMap() {
    // Arrange: stub accountUtil → empty list
    when(accountUtil.getUserAccountIds()).thenReturn(Collections.emptyList());

    // Act
    Map<String, BigDecimal> result = savingsGoalService.getNetMonthlyBalance();

    // Assert
    assertThat(result).isEmpty();
    // transactionRepository should never be called when accountIds.isEmpty()
    verifyNoInteractions(transactionRepository);
    }
    
    @Test
    void getNetMonthlyBalance_someMonthsHaveTransactions_onlyThoseMonthsIncluded() {
    // Arrange
    List<Long> accountIds = List.of(100L, 200L);
    when(accountUtil.getUserAccountIds()).thenReturn(accountIds);

    LocalDate today = LocalDate.now();

    // Pick two offsets, e.g. i=9 (oldest) and i=2, as “months with transactions”
    LocalDate month9 = today.minusMonths(9);
    LocalDate start9 = month9.withDayOfMonth(1);
    LocalDate end9   = month9.withDayOfMonth(month9.lengthOfMonth());
    String label9    = start9.getMonth().toString().substring(0, 3);

    LocalDate month2 = today.minusMonths(2);
    LocalDate start2 = month2.withDayOfMonth(1);
    LocalDate end2   = month2.withDayOfMonth(month2.lengthOfMonth());
    String label2    = start2.getMonth().toString().substring(0, 3);

    // Stub existsByAccountIdInAndDateBetween(...) → true for those two months only
    when(transactionRepository.existsByAccountIdInAndDateBetween(accountIds, start9, end9))
        .thenReturn(true);
    when(transactionRepository.existsByAccountIdInAndDateBetween(accountIds, start2, end2))
        .thenReturn(true);
    // All other months default to false (Mockito returns false)

    // Stub netSavingsMonthlyBalance(...) for those two months
    when(transactionRepository.netSavingsMonthlyBalance(accountIds, start9, end9))
        .thenReturn(new BigDecimal("123.45"));
    when(transactionRepository.netSavingsMonthlyBalance(accountIds, start2, end2))
        .thenReturn(new BigDecimal("67.89"));

    // Act
    Map<String, BigDecimal> result = savingsGoalService.getNetMonthlyBalance();

    // Assert: only two entries, in chronological order (month9 first, then month2)
    assertThat(result)
      .hasSize(2)
      .containsExactly(
        entry(label9, new BigDecimal("123.45")),
        entry(label2, new BigDecimal("67.89"))
      );

    // Verify existsBy... was called for each of the last 10 months
    for (int i = 9; i >= 0; i--) {
      LocalDate m = today.minusMonths(i);
      LocalDate start = m.withDayOfMonth(1);
      LocalDate end   = m.withDayOfMonth(m.lengthOfMonth());
      verify(transactionRepository).existsByAccountIdInAndDateBetween(accountIds, start, end);
    }

    // Verify netSavingsMonthlyBalance(...) was called only for the two “true” months
    verify(transactionRepository).netSavingsMonthlyBalance(accountIds, start9, end9);
    verify(transactionRepository).netSavingsMonthlyBalance(accountIds, start2, end2);
    }
    
    @Test
    void transferFunds_accountNotFound_throws404() {
        // Arrange
        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "Deposit", "nonexistent-account"
        );
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "nonexistent-account"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> savingsGoalService.transferFunds(10L, request))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(rse.getReason()).isEqualTo("Account not found");
            });

        verify(accountRepository).findByUserIdAndNameAndActiveTrue(1L, "nonexistent-account");
        verifyNoMoreInteractions(savingsGoalRepository, categoryRepository, transactionService);
    }

    @Test
    void transferFunds_savingsGoalNotFound_throws404() {
        // Arrange
        Account acct = new Account("test-acc", new BigDecimal("200"), user);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-acc"))
            .thenReturn(Optional.of(acct));
        when(savingsGoalRepository.findById(10L))
            .thenReturn(Optional.empty());

        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "Deposit", "test-acc"
        );

        // Act & Assert
        assertThatThrownBy(() -> savingsGoalService.transferFunds(10L, request))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(rse.getReason()).isEqualTo("Savings goal is missing");
            });

        verify(accountRepository).findByUserIdAndNameAndActiveTrue(1L, "test-acc");
        verify(savingsGoalRepository).findById(10L);
        verifyNoMoreInteractions(categoryRepository, transactionService);
    }

    @Test
    void transferFunds_categoryNotFound_throws404() {
        // Arrange
        Account acct = new Account("test-acc", new BigDecimal("200"), user);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-acc"))
            .thenReturn(Optional.of(acct));
        when(savingsGoalRepository.findById(10L))
            .thenReturn(Optional.of(goal));
        when(categoryRepository.findByName("Fund Transfer"))
            .thenReturn(Optional.empty());

        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "Deposit", "test-acc"
        );

        // Act & Assert
        assertThatThrownBy(() -> savingsGoalService.transferFunds(10L, request))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(rse.getReason()).isEqualTo("internal error");
            });

        verify(accountRepository).findByUserIdAndNameAndActiveTrue(1L, "test-acc");
        verify(savingsGoalRepository).findById(10L);
        verify(categoryRepository).findByName("Fund Transfer");
        verifyNoMoreInteractions(transactionService);
    }
    
    @Test
    void transferFunds_withdrawInsufficientFunds_throwsConflict() {
        // Arrange
        // Account and goal both exist
        Account acct = new Account("test-acc", new BigDecimal("200"), user);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-acc"))
            .thenReturn(Optional.of(acct));
        goal.setCurrentAmount(new BigDecimal("50"));  // less than withdrawal
        when(savingsGoalRepository.findById(10L))
            .thenReturn(Optional.of(goal));
        Category cat = new Category("Fund Transfer");
        cat.setId(5L);
        when(categoryRepository.findByName("Fund Transfer"))
            .thenReturn(Optional.of(cat));

        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "Withdraw", "test-acc"
        );

        // Act & Assert
        assertThatThrownBy(() -> savingsGoalService.transferFunds(10L, request))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(rse.getReason()).isEqualTo("insufficient funds");
            });

        verify(accountRepository).findByUserIdAndNameAndActiveTrue(1L, "test-acc");
        verify(savingsGoalRepository).findById(10L);
        verify(categoryRepository).findByName("Fund Transfer");
        verifyNoMoreInteractions(transactionService);
    }
    
    @Test
    void transferFunds_withdrawSuccess_savesTransactionAndUpdatesGoal() {
        // Arrange
        Account acct = new Account("test-acc", new BigDecimal("200"), user);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-acc"))
            .thenReturn(Optional.of(acct));
        goal.setCurrentAmount(new BigDecimal("150"));
        when(savingsGoalRepository.findById(10L))
            .thenReturn(Optional.of(goal));
        Category cat = new Category("Fund Transfer");
        cat.setId(5L);
        when(categoryRepository.findByName("Fund Transfer"))
            .thenReturn(Optional.of(cat));

        // Stub transactionService.call (void)
        when(transactionService.saveTransaction(any(TransactionCreationRequest.class)))
        .thenReturn(null);
        // Stub save on savingsGoalRepository
        when(savingsGoalRepository.save(any(SavingsGoal.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "Withdraw", "test-acc"
        );

        // Act
        SavingsGoal returned = savingsGoalService.transferFunds(10L, request);

        // Assert: goal currentAmount decreased by 100 (150 → 50)
        assertThat(returned.getCurrentAmount()).isEqualByComparingTo("50");

        // Verify transactionService was called with a deposit transaction
        ArgumentCaptor<TransactionCreationRequest> txCaptor =
            ArgumentCaptor.forClass(TransactionCreationRequest.class);
        verify(transactionService).saveTransaction(txCaptor.capture());
        TransactionCreationRequest txReq = txCaptor.getValue();
        assertThat(txReq.getType()).isEqualTo("Deposit");
        assertThat(txReq.getAmount()).isEqualByComparingTo("100");
        assertThat(txReq.getCategoryId()).isEqualTo(5L);
        assertThat(txReq.getAccountName()).isEqualTo("test-acc");
        assertThat(txReq.getDescription()).isEqualTo("Withdraw from savings");

        // Verify goal was saved with updated currentAmount
        ArgumentCaptor<SavingsGoal> goalCaptor =
            ArgumentCaptor.forClass(SavingsGoal.class);
        verify(savingsGoalRepository).save(goalCaptor.capture());
        SavingsGoal savedGoal = goalCaptor.getValue();
        assertThat(savedGoal.getCurrentAmount()).isEqualByComparingTo("50");
    }

    @Test
    void transferFunds_depositInsufficientFunds_throwsRuntimeException() {
        // Arrange
        Account acct = new Account("test-acc", new BigDecimal("50"), user);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-acc"))
            .thenReturn(Optional.of(acct));
        when(savingsGoalRepository.findById(10L))
            .thenReturn(Optional.of(goal));
        Category cat = new Category("Fund Transfer");
        cat.setId(5L);
        when(categoryRepository.findByName("Fund Transfer"))
            .thenReturn(Optional.of(cat));

        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "Deposit", "test-acc"
        );

        // Act & Assert
        assertThatThrownBy(() -> savingsGoalService.transferFunds(10L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Insufficient funds in account");

        verify(transactionService, never()).saveTransaction(any());
        verify(savingsGoalRepository, never()).save(any());
    }

    @Test
    void transferFunds_depositSuccess_savesTransactionAndUpdatesGoal() {
        // Arrange
        Account acct = new Account("test-acc", new BigDecimal("200"), user);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-acc"))
            .thenReturn(Optional.of(acct));
        goal.setCurrentAmount(new BigDecimal("50"));
        when(savingsGoalRepository.findById(10L))
            .thenReturn(Optional.of(goal));
        Category cat = new Category("Fund Transfer");
        cat.setId(5L);
        when(categoryRepository.findByName("Fund Transfer"))
            .thenReturn(Optional.of(cat));

        when(transactionService.saveTransaction(any(TransactionCreationRequest.class)))
        .thenReturn(null);
        when(savingsGoalRepository.save(any(SavingsGoal.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "Deposit", "test-acc"
        );

        // Act
        SavingsGoal returned = savingsGoalService.transferFunds(10L, request);

        // Assert: goal currentAmount increased by 100 (50 → 150)
        assertThat(returned.getCurrentAmount()).isEqualByComparingTo("150");

        // Verify transactionService was called with an expense transaction
        ArgumentCaptor<TransactionCreationRequest> txCaptor =
            ArgumentCaptor.forClass(TransactionCreationRequest.class);
        verify(transactionService).saveTransaction(txCaptor.capture());
        TransactionCreationRequest txReq = txCaptor.getValue();
        assertThat(txReq.getType()).isEqualTo("Expense");
        assertThat(txReq.getDescription()).isEqualTo("Deposit to savings");
        assertThat(txReq.getAmount()).isEqualByComparingTo("100");
        assertThat(txReq.getCategoryId()).isEqualTo(5L);
        assertThat(txReq.getAccountName()).isEqualTo("test-acc");

        // Verify goal was saved with updated currentAmount
        ArgumentCaptor<SavingsGoal> goalCaptor =
            ArgumentCaptor.forClass(SavingsGoal.class);
        verify(savingsGoalRepository).save(goalCaptor.capture());
        SavingsGoal savedGoal = goalCaptor.getValue();
        assertThat(savedGoal.getCurrentAmount()).isEqualByComparingTo("150");
    }

    @Test
    void transferFunds_invalidType_throwsBadRequest() {
        // Arrange
        Account acct = new Account("test-acc", new BigDecimal("200"), user);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-acc"))
            .thenReturn(Optional.of(acct));
        when(savingsGoalRepository.findById(10L))
            .thenReturn(Optional.of(goal));
        Category cat = new Category("Fund Transfer");
        cat.setId(5L);
        when(categoryRepository.findByName("Fund Transfer"))
            .thenReturn(Optional.of(cat));

        SavingsFundTransferDTO request = new SavingsFundTransferDTO(
            new BigDecimal("100"), "InvalidType", "test-acc"
        );

        // Act & Assert
        assertThatThrownBy(() -> savingsGoalService.transferFunds(10L, request))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(rse.getReason()).isEqualTo("Incorret transaction type");
            });

        verify(transactionService, never()).saveTransaction(any());
        verify(savingsGoalRepository, never()).save(any());
    }

}
