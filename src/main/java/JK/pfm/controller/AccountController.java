package JK.pfm.controller;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.model.User;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.AccountService;
import JK.pfm.service.UserService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private UserRepository userRepository;
    
    //Get all acounts
    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    //Create account
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody AccountCreationRequest request) {
    // Retrieve user by username
    Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
    
    if (userOpt == null) {
        return ResponseEntity.badRequest().build(); // Or return an error message if user not found
    }
    User user = userOpt.get();
    
    // Create a new account with the user entity
    Account account = new Account(request.getName(), request.getAmount(), user);
    Account savedAccount = accountService.saveAccount(account);
    return ResponseEntity.ok(savedAccount);
}

    //Delete account
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
    
    //Update name
    @PatchMapping("/{id}/name")
    public ResponseEntity<Account> updateAccountName(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newName = request.get("name");
        Account updatedAccount = accountService.updateAccountName(id, newName);
        return ResponseEntity.ok(updatedAccount);
    }
    
    //get total balance
    @GetMapping("/balance/{userID}")
    public ResponseEntity<BigDecimal> getTotalBalance(@PathVariable("userID") Long userID){
        BigDecimal balance = accountService.getTotalBalance(userID);
        return ResponseEntity.ok(balance);
    }
}
