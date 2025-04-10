package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
import JK.pfm.util.AccountSpecifications;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    

    
    //getting accounts for user
    public List<Account> getAccountsForUser(Long userId) {
      
        return accountRepository.findAll(AccountSpecifications.belongsToUser(userId));
    }

    //saving account
    public Account saveAccount(Account account) {
        //validations
        Validations.emptyFieldValidation(account.getName(), "Name");
        Validations.numberCheck(account.getAmount(), "Amount");
        Validations.negativeCheck(account.getAmount(), "Amount");
        Validations.checkObj(account, "account");
        Validations.checkObj(account.getUser(), "User");

        if (accountRepository.findByUserIdAndName(account.getUser().getId(), account.getName()).isPresent()) {
            throw new RuntimeException("Account with name " + account.getName() + " already exists.");
        }

        return accountRepository.save(account);
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
        //and existence in db
        Optional<Account> accountOpt = accountRepository.findById(id);
        if (accountOpt.isEmpty()) {
            throw new RuntimeException("Account not found!");
        }
        Long userId = SecurityUtil.getUserId();
        Account account = accountOpt.get();
        if(!account.getUser().getId().equals(userId)){
            throw new RuntimeException("Account not found!");
        }
        account.setName(newName);
        return accountRepository.save(account);
    }
}
