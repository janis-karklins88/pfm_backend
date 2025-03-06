package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
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
}
