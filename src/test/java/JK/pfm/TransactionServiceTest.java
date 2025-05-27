package JK.pfm;

import JK.pfm.dto.TransactionCreationRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.service.TransactionService;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountRepository accountRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock AccountUtil accountUtil;
    
    @InjectMocks TransactionService service;

    private MockedStatic<SecurityUtil> securityUtilMock;
    private Account dummyAccount;
    private Category dummyCategory;

    @BeforeEach
    void setUp() {
        // manually open static mock
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(1L);

        // Prepare dummy account
        dummyAccount = new Account();
        dummyAccount.setId(10L);
        dummyAccount.setName("Checking");
        dummyAccount.setAmount(new BigDecimal("100.00"));
        dummyAccount.setIsActive(true);

        // Prepare dummy category
        dummyCategory = new Category();
        dummyCategory.setId(5L);
        dummyCategory.setName("Food");
    }

    @AfterEach
    void tearDown() {
        // close static mock
        securityUtilMock.close();
    }

    @Test
    void saveExpense_reducesBalance() {
        // Arrange
        TransactionCreationRequest req = new TransactionCreationRequest(
            LocalDate.now(),
            new BigDecimal("30.00"),
            dummyCategory.getId(),
            dummyAccount.getName(),
            "Expense",
            "Lunch"
        );
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "Checking"))
            .thenReturn(java.util.Optional.of(dummyAccount));
        when(categoryRepository.findById(5L))
            .thenReturn(java.util.Optional.of(dummyCategory));
        when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Transaction tx = service.saveTransaction(req);

        // Assert
        assertThat(tx.getAmount()).isEqualByComparingTo("30.00");
        assertThat(dummyAccount.getAmount()).isEqualByComparingTo("70.00");
        verify(transactionRepository).save(tx);
    }

    @Test
    void saveExpense_insufficientFunds_throws() {
        // Arrange
        dummyAccount.setAmount(new BigDecimal("10.00"));
        TransactionCreationRequest req = new TransactionCreationRequest(
            LocalDate.now(),
            new BigDecimal("20.00"),
            dummyCategory.getId(),
            dummyAccount.getName(),
            "Expense",
            "Big purchase"
        );
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "Checking"))
            .thenReturn(java.util.Optional.of(dummyAccount));
        when(categoryRepository.findById(5L))
            .thenReturn(java.util.Optional.of(dummyCategory));

        // Assert
        assertThatThrownBy(() -> service.saveTransaction(req))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Not enough funds");
    }
    
    @Test
void saveDeposit_increasesBalance() {
    // Arrange: a 100.00‐balance account
    TransactionCreationRequest req = new TransactionCreationRequest(
        LocalDate.now(),
        new BigDecimal("25.00"),
        dummyCategory.getId(),
        dummyAccount.getName(),
        "Deposit",
        "Paycheck"
    );
    when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "Checking"))
        .thenReturn(Optional.of(dummyAccount));
    when(categoryRepository.findById(5L))
        .thenReturn(Optional.of(dummyCategory));
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    // Act
    Transaction tx = service.saveTransaction(req);

    // Assert
    // 1) The returned transaction has the right amount
    assertThat(tx.getAmount()).isEqualByComparingTo("25.00");
    // 2) The account’s balance increased from 100 → 125
    assertThat(dummyAccount.getAmount()).isEqualByComparingTo("125.00");
    verify(transactionRepository).save(tx);
}

@Test
void deleteExpense_refundsAccount() {
    // Arrange: account has 50, and we have an expense of 20
    dummyAccount.setAmount(new BigDecimal("50.00"));
    Transaction expense = new Transaction(
        LocalDate.now(),
        new BigDecimal("20.00"),
        dummyAccount,
        dummyCategory,
        "Expense",
        "Dinner"
    );
    expense.setId(99L);

    when(transactionRepository.findById(99L))
        .thenReturn(Optional.of(expense));
    doNothing().when(transactionRepository).deleteById(99L);

    // Act
    service.deleteTransaction(99L);

    // Assert
    // Balance should go from 50 → 70
    assertThat(dummyAccount.getAmount()).isEqualByComparingTo("70.00");
    verify(accountRepository).save(dummyAccount);
    verify(transactionRepository).deleteById(99L);
}

@Test
void deleteDeposit_withdrawsFromAccount() {
    // Arrange: account has 100, and we delete a deposit of 30
    dummyAccount.setAmount(new BigDecimal("100.00"));
    Transaction deposit = new Transaction(
        LocalDate.now(),
        new BigDecimal("30.00"),
        dummyAccount,
        dummyCategory,
        "Deposit",
        "Salary"
    );
    deposit.setId(100L);

    when(transactionRepository.findById(100L))
        .thenReturn(Optional.of(deposit));
    doNothing().when(transactionRepository).deleteById(100L);

    // Act
    service.deleteTransaction(100L);

    // Assert
    // Balance should go from 100 → 70
    assertThat(dummyAccount.getAmount()).isEqualByComparingTo("70.00");
    verify(accountRepository).save(dummyAccount);
    verify(transactionRepository).deleteById(100L);
}

@Test
void deleteProhibitedCategory_throws() {
    // Arrange: set category name to one of the forbidden ones
    dummyCategory.setName("Savings");
    Transaction tx = new Transaction(
        LocalDate.now(), BigDecimal.TEN,
        dummyAccount, dummyCategory,
        "Expense", "Test"
    );
    tx.setId(101L);
    when(transactionRepository.findById(101L))
        .thenReturn(Optional.of(tx));

    // Act & Assert
    assertThatThrownBy(() -> service.deleteTransaction(101L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Deleting prohibited");
}

@Test
void deleteDeposit_insufficientFunds_throws() {
    // Arrange: account has only 10 but deposit was 20
    dummyAccount.setAmount(new BigDecimal("10.00"));
    Transaction deposit = new Transaction(
        LocalDate.now(),
        new BigDecimal("20.00"),
        dummyAccount,
        dummyCategory,
        "Deposit",
        "Test"
    );
    deposit.setId(102L);
    when(transactionRepository.findById(102L))
        .thenReturn(Optional.of(deposit));

    // Act & Assert
    assertThatThrownBy(() -> service.deleteTransaction(102L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Insufficient funds");
}


   
@Test
void saveTransaction_accountMissing_throwsNotFound() {
    TransactionCreationRequest req = new TransactionCreationRequest(
        LocalDate.now(),
        new BigDecimal("25.00"),
        dummyCategory.getId(),
        dummyAccount.getName(),
        "Deposit",
        "Paycheck"
    );

    when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "Checking"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.saveTransaction(req))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Account missing");
}

@Test
void saveTransaction_categoryMissing_throwsNotFound() {
    // Arrange
    TransactionCreationRequest req = new TransactionCreationRequest(
        LocalDate.now(),
        new BigDecimal("25.00"),
        dummyCategory.getId(),     // arbitrary
        dummyAccount.getName(),
        "Deposit",
        "Paycheck"
    );
    
    when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "Checking"))
        .thenReturn(Optional.of(dummyAccount));
    when(categoryRepository.findById(5L))
        .thenReturn(Optional.empty());



    assertThatThrownBy(() -> service.saveTransaction(req))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Category missing");
}

@SuppressWarnings("unchecked")
@Test
void getTransactionsByFilters_delegatesWithSort() {
  List<Transaction> fake = List.of(new Transaction(), new Transaction());

  when(transactionRepository.findAll(
      Mockito.<Specification<Transaction>>any(),
      Mockito.any(org.springframework.data.domain.Sort.class)
    ))
    .thenReturn(fake);

  List<Transaction> out = service.getTransactionsByFilters(
    LocalDate.now(), LocalDate.now(), null, null, 1L, null
  );

  assertThat(out).isSameAs(fake);

  // And verify with the same correct Sort class
  verify(transactionRepository).findAll(
    Mockito.<Specification<Transaction>>any(),
    Mockito.any(org.springframework.data.domain.Sort.class)
  );
}




}
