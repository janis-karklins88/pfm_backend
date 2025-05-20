package JK.pfm.controller;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.ChangeAccountNameDto;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.model.Account;
import JK.pfm.service.AccountService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    
    public AccountController(AccountService service){
        this.accountService = service;
    }
    

    
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
       var savedAccount = accountService.saveAccount(request);
       URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(savedAccount.getId())
        .toUri();
       return ResponseEntity.created(uri).body(savedAccount);
    }

    // Delete account
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
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
