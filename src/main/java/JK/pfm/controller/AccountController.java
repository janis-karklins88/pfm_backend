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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST endpoints for managing user accounts.
 * <p>
 * All endpoints require authentication. Methods act on the currently
 * authenticated user's data and never expose other users accounts.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    
    public AccountController(AccountService service){
        this.accountService = service;
    }
    

    
    /**
    * Retrieves all accounts that belong to the currently authenticated user.
    *
    * <p>Returns a {@code 200 OK} response containing a list of {@link Account}
    * objects. Each account represents a financial source (e.g., bank account,
    * wallet, or credit card) owned by the user.</p>
    *
    * <p>The result is scoped to the authenticated user — no other users
    * accounts are ever returned.</p>
    *
    * @return {@code ResponseEntity} containing a list of {@link Account} instances
    * @implNote Uses {@link SecurityUtil#getUserId()} to determine the current user.
    */
    @GetMapping
    public ResponseEntity<List<Account>> getAccountsForUser() {
        Long userId = SecurityUtil.getUserId();
        List<Account> accounts = accountService.getAccountsForUser(userId);
        return ResponseEntity.ok(accounts);
    }
    
    /**
    * Calculates and returns the total balance across all of the authenticated
    * user's accounts.
    *
    * <p>Responds with {@code 200 OK} and a {@link BigDecimal} value representing
    * the sum of all account balances. If the user has no accounts, returns {@code 0}.</p>
    *
    * <p>This endpoint is primarily used for dashboard summaries or overall
    * financial reports.</p>
    *
    * @return {@code ResponseEntity} containing the total balance as a {@link BigDecimal}
    * @implNote Delegates balance calculation to {@link AccountService#getTotalBalance()}.
    */
    @GetMapping("/total-balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        BigDecimal sum = accountService.getTotalBalance();
        return ResponseEntity.ok(sum);
    }

    /**
    * Creates a new account for the authenticated user.
    *
    * <p>On success returns {@code 201 Created} with the created {@link Account}
    * in the body and a {@code Location} header pointing to
    * {@code /api/accounts/{id}}.</p>
    *
    * @param request the payload containing account creation fields; must be valid
    * @return a response with status {@code 201 Created} and the saved account body
    * @throws jakarta.validation.ConstraintViolationException if validation fails
    * @implNote The resulting resource URI is built from the current request and the new ID.
    */
    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountCreationRequest request) {
       var savedAccount = accountService.saveAccount(request);
       URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(savedAccount.getId())
        .toUri();
       return ResponseEntity.created(uri).body(savedAccount);
    }

    /**
    * Deletes an existing account by its unique identifier.
    *
    * <p>Removes the specified account belonging to the authenticated user.
    * This operation is irreversible — all related data (e.g., transactions)
    * may also be deleted depending on cascading rules.</p>
    *
    * <p>Responds with {@code 204 No Content} on successful deletion.</p>
    *
    * @param id the ID of the account to delete
    * @implNote Delegates to {@link AccountService#deleteAccount(Long)}.
    * The request will silently succeed even if the account was already deleted.
    */
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
    }
    
    /**
    * Updates the name of an existing account.
    *  
    * <p>Allows renaming an account belonging to the authenticated user.
    * Only the account name can be modified through this endpoint; other
    * fields remain unchanged.</p>
    *
    * <p>Responds with {@code 200 OK} and the updated {@link Account} object.</p>
    *
    * <p>Access is restricted — only the owner of the account can perform
    * this operation.</p>
    *
    * @param id the ID of the account to rename
    * @param request a {@link ChangeAccountNameDto} containing the new account name
    * @return {@code ResponseEntity} with the updated {@link Account}
    * @implNote Protected by {@code @PreAuthorize("@securityUtil.isCurrentUserAccount(#id)")}
    * to ensure ownership validation before update.
    */
    @PreAuthorize("@securityUtil.isCurrentUserAccount(#id)")
    @PatchMapping("/{id}/name")
    public ResponseEntity<Account> updateAccountName(@PathVariable Long id, @Valid @RequestBody ChangeAccountNameDto request) {
        Account updatedAccount = accountService.updateAccountName(id, request);
        return ResponseEntity.ok(updatedAccount);
    }
    
   /**
    * Transfers funds to or from the specified account.
    *
    * <p>Handles balance adjustments such as savings transfers or manual
    * fund movements between accounts. The operation validates available
    * funds and transaction direction before applying changes.</p>
    *
    * <p>Responds with {@code 200 OK} and the updated {@link Account}
    * reflecting the new balance.</p>
    *
    * @param id the ID of the account receiving or sending funds
    * @param request a {@link SavingsFundTransferDTO} containing transfer details
    * @return {@code ResponseEntity} with the updated {@link Account} after transfer
    * @implNote Delegates to {@link AccountService#transferAccountFunds(Long, SavingsFundTransferDTO)}.
    * Validation should ensure that negative balances are not allowed unless
    * explicitly supported by the account type.
    */
    @PatchMapping("/{id}/transfer-funds")
    public ResponseEntity<Account> transferAccountFunds(
            @PathVariable Long id, 
            @Valid @RequestBody SavingsFundTransferDTO request) {
        

        Account account = accountService.transferAccountFunds(id, request);
        return ResponseEntity.ok(account);
    }
    

}
