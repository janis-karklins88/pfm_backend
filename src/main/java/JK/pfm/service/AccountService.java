package JK.pfm.service;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.AccountSpecifications;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
      
        return accountRepository.findAll(AccountSpecifications.belongsToUser(userId));
    }

    //saving account
    @Transactional
    public Account saveAccount(AccountCreationRequest request) {
        //validations
        Validations.emptyFieldValidation(request.getName(), "Name");
        Validations.numberCheck(request.getAmount(), "Amount");
        Validations.negativeCheck(request.getAmount(), "Amount");
        
        //get user details
        Long userId = SecurityUtil.getUserId();
        
        //check if account already exist
        if (accountRepository.findByUserIdAndName(userId, request.getName()).isPresent()) {
            throw new RuntimeException("Account with name " + request.getName() + " already exists.");
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
    public void deleteAccount(Long id) {
        Long userId = SecurityUtil.getUserId();
        Account account = accountRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Account not found!"));
        if(!account.getUser().getId().equals(userId)){
            throw new RuntimeException("Account not found!");
        }
        accountRepository.deleteById(id);
    }

    
    //getting total balance
    public BigDecimal getTotalBalance(){
        Long userId = SecurityUtil.getUserId();
        return accountRepository.getTotalBalanceByUserId(userId);
    }
    
    //updating account name
    @Transactional
    public Account updateAccountName(Long id, String newName) {
        //validating field
        Validations.emptyFieldValidation(newName, "Account Name");
        Account account = accountRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Account not found!"));
        if(!account.getUser().getId().equals(SecurityUtil.getUserId())){
            throw new RuntimeException("Account not found!");
        }
        account.setName(newName);
        return accountRepository.save(account);
    }
}
