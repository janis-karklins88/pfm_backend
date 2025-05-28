
package JK.pfm;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.ChangeAccountNameDto;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.AccountService;
import JK.pfm.service.TransactionService;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.util.HashSet;
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
import static org.mockito.Mockito.verify;
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
    private Category stubCategory;
    private Transaction stubTransaction;
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
  
}
