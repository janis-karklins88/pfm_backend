
package JK.pfm.service;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.ChangeAccountNameDto;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.AccountService;
import JK.pfm.service.TransactionService;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.util.HashSet;
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
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AccountServiceTest {
    @Mock AccountRepository    accountRepository;
    @Mock UserRepository       userRepository;
    @Mock CategoryRepository   categoryRepository;
    @Mock TransactionService   transactionService;
  
    @InjectMocks
    AccountService accountService;
    private MockedStatic<SecurityUtil> securityUtilMock;
    private User    stubUser;
    private Account stubAccount;
    private AccountCreationRequest stubRequest;
    
    
    @BeforeEach
    void setUp(){
        securityUtilMock = mockStatic(SecurityUtil.class);
        
        stubUser = new User();
        stubUser.setId(1L);
        stubUser.setUsername("test-user");
        stubUser.setPassword("password");
        stubUser.setCategoryPreferences(new HashSet<>());
        
        securityUtilMock.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(stubUser);
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(1L);
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-account"))
        .thenReturn(Optional.empty());
        
    }
    
    @AfterEach
    void tearDown(){
        securityUtilMock.close();
    }
    
    @Test
    void createAccount_zeroBalance(){
        stubRequest = new AccountCreationRequest("test-account", BigDecimal.ZERO);
        
        when(accountRepository.save(any(Account.class)))
        .thenAnswer(inv -> {
          Account toSave = inv.getArgument(0);
          toSave.setId( 99L );
          return toSave;
        });
        stubAccount = accountService.saveAccount(stubRequest);
        
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        
        Account passedIn = captor.getValue();
        assertThat(passedIn.getName()).isEqualTo("test-account");
        assertThat(passedIn.getAmount()).isEqualByComparingTo("0");
        assertThat(passedIn.getUser()).isSameAs(stubUser);
        assertThat(stubAccount.getId()).isEqualTo(99L);
        verify(transactionService, never()).saveTransaction(any());
        verify(categoryRepository, never()).findIdByName("Opening Balance");
    }
    
    @Test
    void createAccount_positiveBalance_createsOpeningTransaction(){
        when(categoryRepository.findIdByName("Opening Balance"))
        .thenReturn(Optional.of(123L));
        
        var req = new AccountCreationRequest("test-account", new BigDecimal("500"));
        when(accountRepository.save(any(Account.class)))
        .thenAnswer(inv -> {
          Account toSave = inv.getArgument(0);
          toSave.setId( 99L );
          return toSave;
        });
        accountService.saveAccount(req);
        
        ArgumentCaptor<TransactionCreationRequest> txCap =
        ArgumentCaptor.forClass(TransactionCreationRequest.class);
        verify(transactionService).saveTransaction(txCap.capture());
        
        TransactionCreationRequest tx = txCap.getValue();
        assertThat(tx.getAmount()).isEqualByComparingTo("500");
        assertThat(tx.getCategoryId()).isEqualTo(123L);
        assertThat(tx.getAccountName()).isEqualTo("test-account");
    }
    
    @Test
    void createAccount_withDuplicateName_ThrowsConflict(){
        stubRequest = new AccountCreationRequest("test-account", BigDecimal.ZERO);
        when(accountRepository.findByUserIdAndNameAndActiveTrue(1L, "test-account"))
        .thenReturn(Optional.of(new Account()));
        
        assertThatThrownBy(() -> accountService.saveAccount(stubRequest))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(rse.getReason())
        .isEqualTo("Account with this name already exists");
        });

        verify(accountRepository, never()).save(any());
    }
    
    @Test
    void createAccount_positiveBalance_whenOpeningCategoryMissing_throwsNotFound() {
    // given: no “Opening Balance” category
    when(categoryRepository.findIdByName("Opening Balance"))
        .thenReturn(Optional.empty());

    var req = new AccountCreationRequest("test-account", new BigDecimal("500"));

    // expect: a 404 NOT_FOUND
    assertThatThrownBy(() -> accountService.saveAccount(req))
    .isInstanceOf(ResponseStatusException.class)
    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
    .isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void updateAccountName(){
        var updateReq = new ChangeAccountNameDto("new-name");
        Long id = 10L;
        
        // existing account in repo
        Account existing = new Account();
        existing.setId(id);
        existing.setName("old-name");
        when(accountRepository.findById(id))
            .thenReturn(Optional.of(existing));
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), updateReq.getName()))
                .thenReturn(Optional.empty());
        
        Account result = accountService.updateAccountName(id, updateReq);
        verify(accountRepository).save(argThat(acc ->
        acc.getId().equals(id) &&
        acc.getName().equals("new-name")
        ));
        
        
    }
    
    @Test 
    void updateAccountName_nameExists_ThrowsConflict(){

        var updateReq = new ChangeAccountNameDto("test-account");
        Long id = 10L;
        
        when(accountRepository.findById(id))
        .thenReturn(Optional.of(new Account()));
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), updateReq.getName()))
                .thenReturn(Optional.of(new Account()));
        
        assertThatThrownBy(() -> accountService.updateAccountName(id, updateReq))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(
        e -> ((ResponseStatusException)e).getStatusCode(),
        e -> ((ResponseStatusException)e).getReason()
        )
        .containsExactly(
        HttpStatus.CONFLICT,
        "Account name already exists"
        );

    }
    
    @Test
    void updateAccountName_whenAccountMissing_throws404() {
    // given
    long missingId = 999L;
    when(accountRepository.findById(missingId))
        .thenReturn(Optional.empty());
    var updateReq = new ChangeAccountNameDto("new-name");

    // expect
    assertThatThrownBy(() -> accountService.updateAccountName(missingId, updateReq))
    .isInstanceOf(ResponseStatusException.class)
    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
    .isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void deleteAccount_missing_throws404() {
    Long id = 99L;
    // stub “no such active account for this user”
    when(accountRepository.findByUserIdAndIdAndActiveTrue(1L, id))
      .thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.deleteAccount(id))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Account not found");
      });

    // no saves
    verify(accountRepository, never()).save(any());
  }
    
    @Test
    void deleteAccount_withFunds_throws409() {
    Long id = 10L;
    Account acct = new Account();
    acct.setId(id);
    acct.setAmount(new BigDecimal("123.45"));
    acct.setIsActive(true);

    when(accountRepository.findByUserIdAndIdAndActiveTrue(1L, id))
      .thenReturn(Optional.of(acct));

    assertThatThrownBy(() -> accountService.deleteAccount(id))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(rse.getReason())
          .isEqualTo("Account still has funds. Please withdraw funds before deletion.");
      });

    verify(accountRepository, never()).save(any());
  }
    
    @Test
    void deleteAccount_zeroBalance_marksInactive() {
    Long id = 20L;
    Account acct = new Account();
    acct.setId(id);
    acct.setAmount(BigDecimal.ZERO);
    acct.setIsActive(true);

    when(accountRepository.findByUserIdAndIdAndActiveTrue(1L, id))
      .thenReturn(Optional.of(acct));
    when(accountRepository.save(any(Account.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    accountService.deleteAccount(id);

    // after deletion the account should be inactive
    assertThat(acct.getIsActive()).isFalse();

    // and it was saved
    verify(accountRepository).save(argThat(a ->
      a.getId().equals(id) && Boolean.FALSE.equals(a.getIsActive())
    ));
  }
    
    @Test
    void fundTransfer_missingCategory(){
        Long id = 1L;
        var req = new SavingsFundTransferDTO(
                new BigDecimal("100"),
                "Withdraw",
                "test-acc"
        );
        when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.empty());
              
        assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Category not found");
        });
        
    }
    
    @Test
    void fundTransfer_incorrectType(){
        Long id = 1L;
        var req = new SavingsFundTransferDTO(
                new BigDecimal("100"),
                "incorrect-type",
                "test-acc"
        );
        when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));
        
        assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rse.getReason()).isEqualTo("Unknown transfer type: incorrect-type");
        });
        
    }
    
    @Test
    void fundTransfer_deposit_depositAccNotFound(){
        Long id = 1L;
        var req = new SavingsFundTransferDTO(
                new BigDecimal("100"),
                "Deposit",
                "test-acc"
        );
        when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));
        
        when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Account not found");
        });
    }
    
    @Test
    void fundTransfer_deposit_withdrawAccNotFound(){
        Long id = 1L;
        var req = new SavingsFundTransferDTO(
                new BigDecimal("100"),
                "Deposit",
                "test-acc"
        );
        when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));
        
        when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.of(new Account()));
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), req.getAccountName()))
        .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Account not found");
        });
    }
    
    @Test
    void fundTransfer_deposit_insufficientFunds(){
        Long id = 1L;
        var req = new SavingsFundTransferDTO(
                new BigDecimal("100"),
                "Deposit",
                "test-acc"
        );
        Account withdrawAcc = new Account("test-acc", new BigDecimal("50"), SecurityUtil.getUser(userRepository));
        
        when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));
        
        when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.of(new Account()));
        
        when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), req.getAccountName()))
        .thenReturn(Optional.of(withdrawAcc));

        assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(rse.getReason()).isEqualTo("Not enough funds");
        });
    }
    
    @Test
void fundTransfer_withdraw_withdrawAccNotFound() {
    Long id = 1L;
    var req = new SavingsFundTransferDTO(
        new BigDecimal("100"),
        "Withdraw",
        "test-acc"
    );

    // 1) category exists
    when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));

    // 2) withdraw‐account (by ID) does NOT exist
    when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Account not found");
      });
}

@Test
void fundTransfer_withdraw_depositAccNotFound() {
    Long id = 1L;
    var req = new SavingsFundTransferDTO(
        new BigDecimal("100"),
        "Withdraw",
        "test-acc"
    );

    // 1) category exists
    when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));

    // 2) withdraw‐account (by ID) exists
    Account withdrawAcc = new Account("primary-acc", new BigDecimal("500"), SecurityUtil.getUser(userRepository));
    when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.of(withdrawAcc));

    // 3) deposit‐account (by name) does NOT exist
    when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), req.getAccountName()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Account not found");
      });
}

@Test
void fundTransfer_withdraw_insufficientFunds() {
    Long id = 1L;
    var req = new SavingsFundTransferDTO(
        new BigDecimal("100"),
        "Withdraw",
        "test-acc"
    );

    // 1) category exists
    when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));

    // 2) withdraw‐account (by ID) exists but has only 50
    Account withdrawAcc = new Account("primary-acc", new BigDecimal("50"), SecurityUtil.getUser(userRepository));
    when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.of(withdrawAcc));

    // 3) deposit‐account (by name) exists
    Account depositAcc = new Account("test-acc", new BigDecimal("0"), SecurityUtil.getUser(userRepository));
    when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), req.getAccountName()))
        .thenReturn(Optional.of(depositAcc));

    assertThatThrownBy(() -> accountService.transferAccountFunds(id, req))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(rse.getReason()).isEqualTo("Not enough funds");
      });
}
@Test
void fundTransfer_deposit_happyPath(){
    Long id = 1L;
    var req = new SavingsFundTransferDTO(
        new BigDecimal("100"),
        "Deposit",
        "test-acc"
    );
    // 1) category exists
    when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));

    // 2) deposit‐account (by ID)
    Account depositAcc = new Account("primary-acc", new BigDecimal("100"), SecurityUtil.getUser(userRepository));
    when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.of(depositAcc));

    // 3) withdraw‐account (by name) exists
    Account withdrawAcc = new Account("test-acc", new BigDecimal("250"), SecurityUtil.getUser(userRepository));
    when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), req.getAccountName()))
        .thenReturn(Optional.of(withdrawAcc));
    //capture transactions
    ArgumentCaptor<TransactionCreationRequest> txCap = 
    ArgumentCaptor.forClass(TransactionCreationRequest.class);
    
    Account result = accountService.transferAccountFunds(id, req);
    
    assertThat(result).isSameAs(depositAcc);
    verify(transactionService, times(2)).saveTransaction(txCap.capture());
    List<TransactionCreationRequest> txns = txCap.getAllValues();
    
    // First: deposit leg
    TransactionCreationRequest depositTx = txns.get(0);
    assertThat(depositTx.getAmount()).isEqualByComparingTo(req.getAmount());
    assertThat(depositTx.getCategoryId()).isEqualTo(42L);
    assertThat(depositTx.getAccountName()).isEqualTo("primary-acc");
    assertThat(depositTx.getType()).isEqualTo("Deposit");
    assertThat(depositTx.getDescription())
    .isEqualTo("Fund transfer from test-acc");

    // Second: withdraw leg
    TransactionCreationRequest withdrawTx = txns.get(1);
    assertThat(withdrawTx.getAmount()).isEqualByComparingTo(req.getAmount());
    assertThat(withdrawTx.getCategoryId()).isEqualTo(42L);
    assertThat(withdrawTx.getAccountName()).isEqualTo("test-acc");
    assertThat(withdrawTx.getType()).isEqualTo("Expense");
    assertThat(withdrawTx.getDescription())
    .isEqualTo("Withdraw to primary-acc");
    
    verify(categoryRepository).findIdByName("Fund Transfer");
    verifyNoMoreInteractions(transactionService);
    }

    @Test
    void fundTransfer_withdraw_happyPath(){
    Long id = 1L;
    var req = new SavingsFundTransferDTO(
        new BigDecimal("100"),
        "Withdraw",
        "test-acc"
    );
    // 1) category exists
    when(categoryRepository.findIdByName("Fund Transfer"))
        .thenReturn(Optional.of(42L));

    // 2) withdraw‐account (by ID)
    Account withdrawAcc = new Account("primary-acc", new BigDecimal("250"), SecurityUtil.getUser(userRepository));
    when(accountRepository.findByUserIdAndIdAndActiveTrue(SecurityUtil.getUserId(), id))
        .thenReturn(Optional.of(withdrawAcc));

    // 3) deposit‐account (by name) exists
    Account depositAcc = new Account("test-acc", new BigDecimal("250"), SecurityUtil.getUser(userRepository));
    when(accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), req.getAccountName()))
        .thenReturn(Optional.of(depositAcc));
    //capture transactions
    ArgumentCaptor<TransactionCreationRequest> txCap = 
    ArgumentCaptor.forClass(TransactionCreationRequest.class);
    
    Account result = accountService.transferAccountFunds(id, req);
    
    assertThat(result).isSameAs(withdrawAcc);
    verify(transactionService, times(2)).saveTransaction(txCap.capture());
    List<TransactionCreationRequest> txns = txCap.getAllValues();
    
    // First: deposit leg
    TransactionCreationRequest depositTx = txns.get(0);
    assertThat(depositTx.getAmount()).isEqualByComparingTo(req.getAmount());
    assertThat(depositTx.getCategoryId()).isEqualTo(42L);
    assertThat(depositTx.getAccountName()).isEqualTo("test-acc");
    assertThat(depositTx.getType()).isEqualTo("Deposit");
    assertThat(depositTx.getDescription())
    .isEqualTo("Fund transfer from primary-acc");

    // Second: withdraw leg
    TransactionCreationRequest withdrawTx = txns.get(1);
    assertThat(withdrawTx.getAmount()).isEqualByComparingTo(req.getAmount());
    assertThat(withdrawTx.getCategoryId()).isEqualTo(42L);
    assertThat(withdrawTx.getAccountName()).isEqualTo("primary-acc");
    assertThat(withdrawTx.getType()).isEqualTo("Expense");
    assertThat(withdrawTx.getDescription())
    .isEqualTo("Withdraw to test-acc");
    
    verify(categoryRepository).findIdByName("Fund Transfer");
    verifyNoMoreInteractions(transactionService);
    }
  
}
