
package JK.pfm.service;

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
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.RecurringExpenseService;
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
public class RecurringExpenseTest {
    @Mock RecurringExpenseRepository recurringExpenseRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock AccountRepository accountRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    
    @InjectMocks
    RecurringExpenseService recurringExpenseService;
    
    private User user;
    private MockedStatic<SecurityUtil> securityUtilMock;
    private RecurringExpense expense;
    private Account account;
    
    @BeforeEach
    void setUp(){
        securityUtilMock = mockStatic(SecurityUtil.class);
        
        user = new User();
        user.setId(1L);
        
        securityUtilMock.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(user);
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(1L);
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), "test-acc"))
        .thenReturn(Optional.empty());
        when(accountRepository.findByUserIdAndIdAndActiveTrue(1L, 10L))
        .thenReturn(Optional.empty());
        when(recurringExpenseRepository.findById(2L))
        .thenReturn(Optional.empty());
        
        expense = new RecurringExpense("test-payment", new BigDecimal("250"), LocalDate.now(), "MONTHLY", new Account(), new Category());
        expense.setId(42L);
        account = new Account("test-acc", new BigDecimal("200"), SecurityUtil.getUser(userRepository));
        
    }
    
    @AfterEach
    void tearDown(){
        securityUtilMock.close();
    }
    
    @Test
    void savePayment_accountMissing(){
        var request = new RecurringExpenseCreation();
        request.setAccountName("test-acc");
        
        assertThatThrownBy(() -> recurringExpenseService.saveRecurringExpense(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason())
        .isEqualTo("Account missing");
        });

        verify(accountRepository).findByUserIdAndNameAndActiveTrue(1L, "test-acc");
        verify(categoryRepository, never()).findById(anyLong());
        verify(recurringExpenseRepository, never()).save(any());
    }
    
    @Test
    void savePayment_categoryMissing(){
        var request = new RecurringExpenseCreation();
        request.setAccountName("test-acc");
        request.setCategoryId(100L);
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), "test-acc"))
        .thenReturn(Optional.of(account));
        when(categoryRepository.findById(100L))
        .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> recurringExpenseService.saveRecurringExpense(request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason())
        .isEqualTo("Category missing");
        });

        verify(accountRepository).findByUserIdAndNameAndActiveTrue(1L, "test-acc");
        verify(categoryRepository).findById(100L);
        verify(recurringExpenseRepository, never()).save(any());
    }
    
    @Test
    void savePayment_happy(){
        var request = new RecurringExpenseCreation();
        request.setName("monthly-payment");
        request.setAccountName("test-acc");
        request.setAmount(new BigDecimal("250"));
        request.setCategoryId(10L);
        request.setFrequency("MONTHLY");
        request.setStartDate(LocalDate.now());
        
        Category cat = new Category("test-cat");
        cat.setId(10L);
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), "test-acc"))
        .thenReturn(Optional.of(account));
        when(categoryRepository.findById(10L))
        .thenReturn(Optional.of(cat));
        when(recurringExpenseRepository.save(any(RecurringExpense.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        
        RecurringExpense saved = recurringExpenseService.saveRecurringExpense(request);
        
        assertThat(saved.getName()).isEqualTo("monthly-payment");
        assertThat(saved.getAmount()).isEqualByComparingTo("250");
        assertThat(saved.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(saved.getFrequency()).isEqualTo("MONTHLY");
        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getCategory()).isSameAs(cat);
        assertThat(saved.getActive()).isTrue();
        
        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository).save(captor.capture());
        RecurringExpense toSave = captor.getValue();
        
        assertThat(toSave.getName()).isEqualTo("monthly-payment");
        assertThat(toSave.getAmount()).isEqualByComparingTo("250");
        assertThat(toSave.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(toSave.getFrequency()).isEqualTo("MONTHLY");
        assertThat(toSave.getAccount()).isSameAs(account);
        assertThat(toSave.getCategory()).isSameAs(cat);
        assertThat(toSave.getActive()).isTrue();
    }
    
    @Test
    void updateRecurringExpenseAmount_notFound_throws404() {
    UpdatePaymentAmountDto request = new UpdatePaymentAmountDto(new BigDecimal("300"));

    // Act & Assert
    assertThatThrownBy(() -> recurringExpenseService.updateRecurringExpenseAmount(expense.getId(), request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Payment is missing");
        });

    verify(recurringExpenseRepository, never()).save(any());
    }

    @Test
    void updateRecurringExpenseAmount_happyPath_savesAndReturnsUpdated() {
   
    when(recurringExpenseRepository.findById(42L))
        .thenReturn(Optional.of(expense));
    when(recurringExpenseRepository.save(any(RecurringExpense.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    UpdatePaymentAmountDto request = new UpdatePaymentAmountDto(new BigDecimal("75"));

    // Act
    RecurringExpense updated = recurringExpenseService.updateRecurringExpenseAmount(42L, request);

    // Assert: the returned object has the new amount
    assertThat(updated.getAmount()).isEqualByComparingTo("75");

    // Verify that save(...) was called with the same instance and new amount
    ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
    verify(recurringExpenseRepository).save(captor.capture());
    RecurringExpense toSave = captor.getValue();

    assertThat(toSave.getId()).isEqualTo(42L);
    assertThat(toSave.getAmount()).isEqualByComparingTo("75");
    }
    
    @Test
    void updateRecurringExpenseNextDueDate_notFound_throws404() {

        UpdatePaymentNextDueDateDto request = new UpdatePaymentNextDueDateDto(
            LocalDate.of(2025, 7, 1)
        );

        // Act & Assert
        assertThatThrownBy(() ->
            recurringExpenseService.updateRecurringExpenseNextDueDate(999L, request)
        )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Payment is missing");
        });

        // Verify that save(...) was never called
        verify(recurringExpenseRepository, never()).save(any());
    }
    
    @Test
    void updateRecurringExpenseNextDueDate_happyPath_updatesAndSaves() {
        // Arrange
        UpdatePaymentNextDueDateDto request = new UpdatePaymentNextDueDateDto(
            LocalDate.of(2025, 7, 1)
        );

        // Stub findById(42L) to return our shared expense
        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.of(expense));

        // Stub save(...) to echo back the expense
        when(recurringExpenseRepository.save(any(RecurringExpense.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        RecurringExpense returned = recurringExpenseService.updateRecurringExpenseNextDueDate(42L, request);

        // Assert: the returned object has the new nextDueDate
        assertThat(returned.getNextDueDate()).isEqualTo(request.getNextDueDate());

        // Verify that save(...) was called with the same instance, now updated
        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository).save(captor.capture());
        RecurringExpense toSave = captor.getValue();

        assertThat(toSave.getId()).isEqualTo(42L);
        assertThat(toSave.getNextDueDate()).isEqualTo(request.getNextDueDate());
    }
    
    @Test
    void updateRecurringExpenseAccount_incorrectAccount_throws404() {
        // Arrange: request says change to accountId=999L
        UpdateRecurringExpenseAccountDto request = new UpdateRecurringExpenseAccountDto(999L);

        // Stub account lookup to return empty
        when(accountRepository.findByUserIdAndIdAndActiveTrue(1L, 999L))
            .thenReturn(Optional.empty());


        // Act & Assert
        assertThatThrownBy(() ->
            recurringExpenseService.updateRecurringExpenseAccount(42L, request)
        )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Incorrect account");
        });

        // Verify that we never attempted to load the expense or save
        verify(accountRepository).findByUserIdAndIdAndActiveTrue(1L, 999L);
        verify(recurringExpenseRepository, never()).findById(anyLong());
        verify(recurringExpenseRepository, never()).save(any());
    }
    
    @Test
    void updateRecurringExpenseAccount_incorrectPayment_throws404() {
        // Arrange: request says change to accountId=100L
        UpdateRecurringExpenseAccountDto request = new UpdateRecurringExpenseAccountDto(100L);

        // Stub account lookup to succeed
        when(accountRepository.findByUserIdAndIdAndActiveTrue(1L, 100L))
            .thenReturn(Optional.of(account));

        // Stub expense lookup to be missing
        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            recurringExpenseService.updateRecurringExpenseAccount(42L, request)
        )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Incorrect payment");
        });

        verify(accountRepository).findByUserIdAndIdAndActiveTrue(1L, 100L);
        verify(recurringExpenseRepository).findById(42L);
        verify(recurringExpenseRepository, never()).save(any());
    }
    
    @Test
    void updateRecurringExpenseAccount_happyPath_updatesAndSaves() {
        // Arrange: request says change to accountId=100L
        UpdateRecurringExpenseAccountDto request = new UpdateRecurringExpenseAccountDto(100L);

        // Stub account lookup to succeed
        when(accountRepository.findByUserIdAndIdAndActiveTrue(1L, 100L))
            .thenReturn(Optional.of(account));

        // Stub expense lookup to return our shared expense
        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.of(expense));

        // Stub save(...) to echo back the expense
        when(recurringExpenseRepository.save(any(RecurringExpense.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        RecurringExpense returned = recurringExpenseService.updateRecurringExpenseAccount(42L, request);

        // Assert: the returned object now has account == our stub account
        assertThat(returned.getAccount()).isSameAs(account);

        // Verify that save(...) got called with the same instance, updated
        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository).save(captor.capture());
        RecurringExpense toSave = captor.getValue();

        assertThat(toSave.getId()).isEqualTo(42L);
        assertThat(toSave.getAccount()).isSameAs(account);
    }
    
    @Test
    void pauseRecurringExpense_notFound_throws404() {
        // Arrange: make findById(999L) return empty
        when(recurringExpenseRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            recurringExpenseService.pauseRecurringExpense(999L)
        )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Incorrect payment");
        });

        // Verify no save call
        verify(recurringExpenseRepository, never()).save(any());
    }

    @Test
    void pauseRecurringExpense_happyPath_setsActiveFalseAndSaves() {
        // Arrange
        // Stub findById(42L) to return our shared 'expense'
        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.of(expense));
        // By default, expense.isActive() == true (constructor did not explicitly set it to false).

        // Stub save(...) to simply return its argument
        when(recurringExpenseRepository.save(any(RecurringExpense.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        RecurringExpense returned = recurringExpenseService.pauseRecurringExpense(42L);

        // Assert: returned object is the same instance and now inactive
        assertThat(returned).isSameAs(expense);
        assertThat(returned.getActive()).isFalse();

        // Verify that save(...) was called with the updated instance
        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository).save(captor.capture());
        RecurringExpense toSave = captor.getValue();

        assertThat(toSave.getId()).isEqualTo(42L);
        assertThat(toSave.getActive()).isFalse();
    }
    
    @Test
    void resumeRecurringExpense_notFound_throws404() {
        // Arrange: findById(999L) → empty
        when(recurringExpenseRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            recurringExpenseService.resumeRecurringExpense(999L)
        )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Payment not found");
        });

        verify(recurringExpenseRepository, never()).save(any());
    }

    @Test
    void resumeRecurringExpense_unsupportedFrequency_throwsBadRequest() {
        // Arrange: make an expense whose frequency is unsupported (e.g. "DAILY")
        expense.setFrequency("DAILY");
        expense.setLastPayment(LocalDate.of(2025, 1, 1));
        expense.setNextDueDate(null);
        expense.setActive(false);

        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.of(expense));

        // Act & Assert
        assertThatThrownBy(() ->
            recurringExpenseService.resumeRecurringExpense(42L)
        )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(rse.getReason()).isEqualTo("Unsupported frequency");
        });

        verify(recurringExpenseRepository, never()).save(any());
    }

    @Test
    void resumeRecurringExpense_lastPaymentNull_setsActiveTrueWithoutNextDueDate() {
        // Arrange: lastPayment == null, frequency valid but shouldn’t matter
        expense.setFrequency("MONTHLY");
        expense.setLastPayment(null);
        expense.setNextDueDate(null);  // initially null
        expense.setActive(false);

        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.of(expense));
        when(recurringExpenseRepository.save(any(RecurringExpense.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        RecurringExpense returned = recurringExpenseService.resumeRecurringExpense(42L);

        // Assert: active is true; nextDueDate remains null (since base was null)
        assertThat(returned.getActive()).isTrue();
        assertThat(returned.getNextDueDate()).isNull();

        // Verify save(...) called
        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository).save(captor.capture());
        RecurringExpense toSave = captor.getValue();

        assertThat(toSave.getId()).isEqualTo(42L);
        assertThat(toSave.getActive()).isTrue();
        assertThat(toSave.getNextDueDate()).isNull();
    }

    @Test
    void resumeRecurringExpense_happyPath_computesNextDueDateAndActivates() {
        // Arrange:
        // 1) Use a fixed “today” for deterministic behavior
        LocalDate today = LocalDate.now();

        // 2) Set lastPayment two periods before “today” for a weekly frequency:
        expense.setFrequency("WEEKLY");
        expense.setLastPayment(today.minusWeeks(2)); 
        expense.setNextDueDate(null);  // will be overwritten
        expense.setActive(false);

        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.of(expense));
        when(recurringExpenseRepository.save(any(RecurringExpense.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        RecurringExpense returned = recurringExpenseService.resumeRecurringExpense(42L);

        // Compute expected “nextDueDate”:
        // starting at base = today.minusWeeks(2), step = Period.ofWeeks(1)
        // first next = (today.minusWeeks(2)).plusWeeks(1) = today.minusWeeks(1)
        // loop: since that is not after today, next = next.plusWeeks(1) = today
        // loop again: since “today” is not after “today”, next = today.plusWeeks(1)
        LocalDate expectedNext = today.plusWeeks(1);

        // Assert:
        assertThat(returned.getActive()).isTrue();
        assertThat(returned.getNextDueDate()).isEqualTo(expectedNext);

        // Verify save(...) called with the updated instance
        ArgumentCaptor<RecurringExpense> captor = ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository).save(captor.capture());
        RecurringExpense toSave = captor.getValue();

        assertThat(toSave.getId()).isEqualTo(42L);
        assertThat(toSave.getActive()).isTrue();
        assertThat(toSave.getNextDueDate()).isEqualTo(expectedNext);
    }
    
    @Test
    void deleteRecurringExpense_notFound_throws404() {

        // Act & Assert
        assertThatThrownBy(() ->
            recurringExpenseService.deleteRecurringExpense(999L)
        )
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(rse.getReason()).isEqualTo("Payment not found");
        });

        // Verify deleteById(...) was never called
        verify(recurringExpenseRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteRecurringExpense_happyPath_deletesById() {
        // Arrange: stub findById(42L) → shared expense
        when(recurringExpenseRepository.findById(42L))
            .thenReturn(Optional.of(expense));

        // Act
        recurringExpenseService.deleteRecurringExpense(42L);

        // Assert: deleteById(42L) was called exactly once
        verify(recurringExpenseRepository).deleteById(42L);
    }
    
    @Test
    void processRecurringExpenses_noDueExpenses_noActions() {
        // Arrange: compute today
        LocalDate today = LocalDate.now();

        // Stub findByNextDueDateLessThanEqual(today) → empty list
        when(recurringExpenseRepository.findByNextDueDateLessThanEqualAndActiveTrue(today))
            .thenReturn(Collections.emptyList());

        // Act
        recurringExpenseService.processRecurringExpenses();

        // Assert: No saves on transaction, account, or recurringExpenseRepository
        verify(recurringExpenseRepository).findByNextDueDateLessThanEqualAndActiveTrue(today);
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
        verify(recurringExpenseRepository, never()).save(any(RecurringExpense.class));
    }
    
    @Test
    void processRecurringExpenses_singleDueExpense_createsTransactionUpdatesAndSaves() {
        // Arrange
        LocalDate today = LocalDate.now();

        // Prepare the expense fixture:
        // - nextDueDate = today (so it is due)
        // - frequency = "MONTHLY"
        // - lastPayment = null (irrelevant here, will be set)
        Category cat = new Category("utilities");
        cat.setId(20L);
        account.setAmount(new BigDecimal("500")); // initial balance

        expense.setNextDueDate(today);
        expense.setFrequency("MONTHLY");
        expense.setCategory(cat);
        expense.setName("rent");
        expense.setAmount(new BigDecimal("100"));
        expense.setAccount(account);
        expense.setLastPayment(null);

        // Stub repository to return this one expense
        when(recurringExpenseRepository.findByNextDueDateLessThanEqualAndActiveTrue(today))
            .thenReturn(List.of(expense));

        // Stub transactionRepository.save(...) to echo back the transaction
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        // Stub accountRepository.save(...) to echo back the account
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        // Stub recurringExpenseRepository.save(...) to echo back the expense
        when(recurringExpenseRepository.save(any(RecurringExpense.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        recurringExpenseService.processRecurringExpenses();

        // 1) Verify transaction saved
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getAccount()).isSameAs(account);
        assertThat(savedTx.getAmount()).isEqualByComparingTo("100");
        assertThat(savedTx.getCategory()).isSameAs(cat);
        assertThat(savedTx.getType()).isEqualTo("Expense");
        assertThat(savedTx.getDate()).isEqualTo(today);
        assertThat(savedTx.getDescription()).isEqualTo("MONTHLY payment: rent");

        // 2) Verify account balance updated and saved
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        // original 500 − 100 = 400
        assertThat(savedAccount.getAmount()).isEqualByComparingTo("400");

        // 3) Verify expense's nextDueDate and lastPayment updated, then saved
        ArgumentCaptor<RecurringExpense> expenseCaptor =
            ArgumentCaptor.forClass(RecurringExpense.class);
        verify(recurringExpenseRepository).save(expenseCaptor.capture());
        RecurringExpense savedExpense = expenseCaptor.getValue();
        // nextDueDate: today.plusMonths(1)
        LocalDate expectedNext = today.plusMonths(1);
        assertThat(savedExpense.getNextDueDate()).isEqualTo(expectedNext);
        assertThat(savedExpense.getLastPayment()).isEqualTo(today);
    }

}
