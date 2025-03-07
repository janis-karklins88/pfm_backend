package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
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

    //getting accounts
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    //saving account
    public Account saveAccount(Account account) {
        //validations
        Validations.emptyFieldValidation(account.getName(), "Name");
        Validations.numberCheck(account.getAmount(), "Amount");
        
        return accountRepository.save(account);
    }

    //deleting account
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    //getting one account 
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }
    
    //getting total balance
    public BigDecimal getTotalBalance(Long id){
        return accountRepository.getTotalBalanceByUserId(id);
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
        
        Account account = accountOpt.get();
        account.setName(newName);
        return accountRepository.save(account);
    }
}
