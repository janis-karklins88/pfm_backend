package JK.pfm.controller;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.model.User;
import JK.pfm.repository.UserRepository;
import JK.pfm.security.CustomUserDetails;
import JK.pfm.service.AccountService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private UserRepository userRepository;
    
    // Get all accounts for the authenticated user
    @GetMapping
    public ResponseEntity<List<Account>> getAccountsForUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        List<Account> accounts = accountService.getAccountsForUser(userId);
        return ResponseEntity.ok(accounts);
    }

    // Create a new account for the authenticated user
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody AccountCreationRequest request) {
        // Retrieve authenticated user's username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // Lookup the full user entity based on the username
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        User user = userOpt.get();
        
        // Create a new account associated with the authenticated user
        Account account = new Account(request.getName(), request.getAmount(), user);
        Account savedAccount = accountService.saveAccount(account);
        return ResponseEntity.ok(savedAccount);
    }

    // Delete account
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
    
    // Update account name
    @PatchMapping("/{id}/name")
    public ResponseEntity<Account> updateAccountName(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newName = request.get("name");
        Account updatedAccount = accountService.updateAccountName(id, newName);
        return ResponseEntity.ok(updatedAccount);
    }
    
    // Get total balance for the authenticated user
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        BigDecimal balance = accountService.getTotalBalance(userDetails.getId());
        return ResponseEntity.ok(balance);
    }
}
