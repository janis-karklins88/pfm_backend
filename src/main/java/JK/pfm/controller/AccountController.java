package JK.pfm.controller;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.ChangeAccountNameDto;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.model.Account;
import JK.pfm.service.AccountService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;
    

    
    // Get all accounts for the authenticated user
    @GetMapping
    public ResponseEntity<List<Account>> getAccountsForUser() {
        Long userId = SecurityUtil.getUserId();
        List<Account> accounts = accountService.getAccountsForUser(userId);
        return ResponseEntity.ok(accounts);
    }
    
    // Get total account balance for user
    @GetMapping("/total-balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        BigDecimal sum = accountService.getTotalBalance();
        return ResponseEntity.ok(sum);
    }

    // Create a new account for the authenticated user
    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        Account savedAccount = accountService.saveAccount(request);
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
    public ResponseEntity<Account> updateAccountName(@PathVariable Long id, @Valid @RequestBody ChangeAccountNameDto request) {
        Account updatedAccount = accountService.updateAccountName(id, request);
        return ResponseEntity.ok(updatedAccount);
    }
    
    //transfer funds
    @PatchMapping("/{id}/transfer-funds")
    public ResponseEntity<Account> transferAccountFunds(
            @PathVariable Long id, 
            @Valid @RequestBody SavingsFundTransferDTO request) {
        

        Account account = accountService.transferAccountFunds(id, request);
        return ResponseEntity.ok(account);
    }
    

}
