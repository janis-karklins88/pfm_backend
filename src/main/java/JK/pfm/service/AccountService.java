package JK.pfm.service;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.ChangeAccountNameDto;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.SecurityUtil;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionService transactionService;
    

    
    //getting accounts for user
    public List<Account> getAccountsForUser(Long userId) {
      
        return accountRepository.findByUserIdAndActiveTrue(userId);
    }

    //saving account
    @Transactional
    public Account saveAccount(AccountCreationRequest request) {

        Long userId = SecurityUtil.getUserId();
        
        //check if account already exist
        if (accountRepository.findByUserIdAndNameAndActiveTrue(userId, request.getName()).isPresent()) {
            throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Account with this name already exists"
            );
        }
        
        Account account = new Account(
        request.getName(),
        BigDecimal.ZERO,
        SecurityUtil.getUser(userRepository));
        
        Account saved = accountRepository.save(account);
        
       
        //check for amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            Category misc = categoryRepository
            .findByName("Opening Balance")
            .orElseThrow(() -> new IllegalStateException("category missing"));
      
            Transaction opening = new Transaction(
            LocalDate.now(),
            request.getAmount(),
            saved,
            misc,
            "Deposit",
            "Initial account opening");
            transactionService.saveTransaction(opening);
    }       

        return saved;
    }

    //deleting account
    @Transactional
    public void deleteAccount(Long id) {
        Long userId = SecurityUtil.getUserId();
        Account account = accountRepository.findByUserIdAndIdAndActiveTrue(userId, id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
        
        //check for funds
        if(account.getAmount().compareTo(BigDecimal.ZERO) != 0){
            throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Account still has funds. Please withdraw funds before deletion."
            );
        }
        
        //set incative
        account.setIsActive(false);
        
        accountRepository.save(account);
    }

    
    //getting total balance
    public BigDecimal getTotalBalance(){
        Long userId = SecurityUtil.getUserId();
        return accountRepository.getTotalBalanceByUserId(userId);
    }
    
    //updating account name
    @Transactional
    public Account updateAccountName(Long id, ChangeAccountNameDto request) {
        Long userId = SecurityUtil.getUserId();
        
        Account account = accountRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
        
        if(!account.getUser().getId().equals(userId)){
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Account not found"
            );
        }
        
        accountRepository.findByUserIdAndNameAndActiveTrue(userId, request.getName())
        .ifPresent(a -> {
            throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Account name already exists"
            );
        });
        
        account.setName(request.getName());
        return accountRepository.save(account);
    }
    
    //account funds transfer
    @Transactional
    public Account transferAccountFunds(Long accountId, SavingsFundTransferDTO request){
        
        //get user
        Long userId = SecurityUtil.getUserId();
        //get cat
       Category category = categoryRepository.findByName("Fund Transfer")
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Fund Transfer category is missing"
        ));
       //get date
        LocalDate date = LocalDate.now();
        //amount
        BigDecimal amount = request.getAmount();       
        
        //setting accounts
        Account depositAccount;
        Account withdrawAccount;
        
        if(request.getType().equals("Deposit")){
            depositAccount = accountRepository.findByUserIdAndIdAndActiveTrue(userId, accountId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
            
            withdrawAccount = accountRepository.findByUserIdAndNameAndActiveTrue(userId, request.getAccountName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
            
            //fund check
            if (withdrawAccount.getAmount().compareTo(amount) < 0){
                throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Not enough funds"
            );
            }
        } else if (request.getType().equals("Withdraw")) {
             withdrawAccount = accountRepository.findByUserIdAndIdAndActiveTrue(userId, accountId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
            
            depositAccount = accountRepository.findByUserIdAndNameAndActiveTrue(userId, request.getAccountName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
            
            //fund check
            if (withdrawAccount.getAmount().compareTo(amount) < 0){
                throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Not enough funds"
            );
            }
        } else {
            throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
             "Unknown transfer type: " + request.getType()
            );

        }
        
        //creating transactions
        Transaction depositTransaction = 
            new Transaction(date, amount, depositAccount, category, "Deposit", "Fund transfer from " + withdrawAccount.getName());
            transactionService.saveTransaction(depositTransaction);
            
        Transaction withdrawTransaction = 
            new Transaction(date, amount, withdrawAccount, category, "Expense", "Withdraw to " + depositAccount.getName());
            transactionService.saveTransaction(withdrawTransaction);
            
            
            Account targetAccount = accountRepository.findByUserIdAndIdAndActiveTrue(userId, accountId)
                    .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
        return targetAccount;
        
    }
}
