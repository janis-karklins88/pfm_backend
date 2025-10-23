package JK.pfm.service;

import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.ChangeAccountNameDto;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.model.Account;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.util.SecurityUtil;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TransactionService transactionService
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.transactionService = transactionService;
    }
    

    
    /**
    * Returns all active accounts for the specified user.
    *
    * @param userId the ID of the user whose accounts to fetch
    * @return a list of active {@link JK.pfm.model.Account} records
    */
    public List<Account> getAccountsForUser(Long userId) {
        return accountRepository.findByUserIdAndActiveTrue(userId);
    }

    /**
    * Creates a new account for the currently authenticated user.
    * <p>
    * If the requested initial amount is greater than zero, an opening balance
    * transaction is created using the {@code "Opening Balance"} category.
    *
    * @param request the account creation payload (name and optional initial amount)
    * @return the persisted {@link JK.pfm.model.Account}
    * @throws org.springframework.web.server.ResponseStatusException
    *         if an active account with the same name already exists (409 CONFLICT) or
    *         if the opening balance category cannot be found (404 NOT FOUND)
    */
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
            Long categoryId = categoryRepository.findIdByName("Opening Balance").
                orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category not found"
            ));
      
            TransactionCreationRequest opening = new TransactionCreationRequest(
            LocalDate.now(),
            request.getAmount(),
            categoryId,
            request.getName(),
            "Deposit",
            "Initial account opening");
            transactionService.saveTransaction(opening);
    }       

        return saved;
    }

    /**
    * Deletes (soft-deactivates) an account by ID for the currently authenticated user.
    * <p>
    * The account must have a zero balance; otherwise, deletion is rejected.
    *  
    * @param id the ID of the account to delete
    * @throws org.springframework.web.server.ResponseStatusException
    *         if the account is not found or inactive (404 NOT FOUND), or
    *         if the account still has funds (409 CONFLICT)
    */
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

    
    /**
    * Returns the total balance across all active accounts for the
    * currently authenticated user.
    *
    * @return the aggregated {@link java.math.BigDecimal} balance
    */
    public BigDecimal getTotalBalance(){
        Long userId = SecurityUtil.getUserId();
        return accountRepository.getTotalBalanceByUserId(userId);
    }
    
    /**
    * Renames an account for the currently authenticated user.
    * <p>
    * Name must be unique among the user's active accounts.
    *
    * @param id the ID of the account to rename
    * @param request the payload containing the new account name
    * @return the updated {@link JK.pfm.model.Account}
    * @throws org.springframework.web.server.ResponseStatusException
    *         if the account is not found (404 NOT FOUND) or
    *         if another active account already uses the requested name (409 CONFLICT)
    */
    @PreAuthorize("@securityUtil.isCurrentUserAccount(#id)")
    @Transactional
    public Account updateAccountName(Long id, ChangeAccountNameDto request) {
        Long userId = SecurityUtil.getUserId();
        
        Account account = accountRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));

        
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
    
    /**
    * Transfers funds between two of the user's accounts by creating a pair of transactions.
    * <p>
    * Depending on {@code request.type}:
    * <ul>
    *   <li><b>Deposit</b>: deposits into {@code accountId} and withdraws from {@code request.accountName}</li>
    *   <li><b>Withdraw</b>: withdraws from {@code accountId} and deposits into {@code request.accountName}</li>
    * </ul>
    * The {@code "Fund Transfer"} category is used for both transactions. Insufficient funds
    * on the withdrawal side are rejected.
    *
    * @param accountId the primary account ID (deposit target if type = "Deposit"; withdrawal source if type = "Withdraw")
    * @param request the transfer details (counterparty account name, amount, and type)
    * @return the account affected on the primary side of the operation
    *         (deposit target for "Deposit", withdrawal source for "Withdraw")
    * @throws org.springframework.web.server.ResponseStatusException
    *         if any referenced account is not found (404 NOT FOUND),
    *         if the transfer category is not found (404 NOT FOUND),
    *         if funds are insufficient (409 CONFLICT), or
    *         if the transfer type is unknown (400 BAD REQUEST)
    */
    @Transactional
    public Account transferAccountFunds(Long accountId, SavingsFundTransferDTO request){
        
        //get user
        Long userId = SecurityUtil.getUserId();
        //get category Id
        Long categoryId = categoryRepository.findIdByName("Fund Transfer").
                orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category not found"
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
        TransactionCreationRequest depositTransaction = 
            new TransactionCreationRequest(date, amount, categoryId, depositAccount.getName(), "Deposit", "Fund transfer from " + withdrawAccount.getName());
            transactionService.saveTransaction(depositTransaction);
            
        TransactionCreationRequest withdrawTransaction = 
            new TransactionCreationRequest(date, amount, categoryId, withdrawAccount.getName(), "Expense", "Withdraw to " + depositAccount.getName());
            transactionService.saveTransaction(withdrawTransaction);
            
        return request.getType().equals("Deposit")
            ? depositAccount : withdrawAccount;
        
    }
}
