package JK.pfm.service;

import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.dto.UpdateSavingsAmountDto;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.SavingsGoal;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.specifications.SavingsGoalSpecification;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;
    private final AccountUtil accountUtil;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public SavingsGoalService(
            SavingsGoalRepository savingsGoalRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            TransactionService transactionService,
            AccountUtil accountUtil,
            UserRepository userRepository,
            AccountRepository accountRepository
    ) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository    = categoryRepository;
        this.transactionService    = transactionService;
        this.accountUtil           = accountUtil;
        this.userRepository        = userRepository;
        this.accountRepository     = accountRepository;
    }

    //getting all saving goals
    public List<SavingsGoal> getAllSavingsGoals(Long userId) {
        Specification<SavingsGoal> spec = Specification.where(SavingsGoalSpecification.belongsToUser(userId));
        return savingsGoalRepository.findAll(spec);
    }
    
    //getting total balance
    public BigDecimal getTotalBalance(){
        Long userId = SecurityUtil.getUserId();
        return savingsGoalRepository.getTotalBalanceByUserId(userId);
    }

    //saving saving goals
    public SavingsGoal saveSavingsGoal(SavingGoalCreation request) {
        SavingsGoal goal = new SavingsGoal(
                request.getName(), 
                request.getTargetAmount(), 
                request.getDescription(), 
                SecurityUtil.getUser(userRepository)
                );
        return savingsGoalRepository.save(goal);
    }

    //getting saving goal
    public Optional<SavingsGoal> getSavingsGoalById(Long id) {
        return savingsGoalRepository.findById(id);
    }
    

    //deleting saving goal
    @PreAuthorize("@securityUtil.isCurrentUserSavingsGoal(#id)")
    @Transactional
    public void deleteSavingsGoal(Long id) {
        SavingsGoal savingsGoal = savingsGoalRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Savings goal is missing"
            ));
    
        // Check if there is any balance remaining
        if (savingsGoal.getCurrentAmount().compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Savings goal still has funds. Please withdraw funds before deletiont"
            );
        }
    
    savingsGoalRepository.deleteById(id);
}
    
    //update goal amount
    @PreAuthorize("@securityUtil.isCurrentUserSavingsGoal(#id)")
    public SavingsGoal updateSavingsGoalAmount(Long id, UpdateSavingsAmountDto amount) {
        
        SavingsGoal savingsGoal = savingsGoalRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Savings goal is missing"
            ));

        savingsGoal.setTargetAmount(amount.getAmount());
        return savingsGoalRepository.save(savingsGoal);
    }
    
    //transfer funds
    @PreAuthorize("@securityUtil.isCurrentUserSavingsGoal(#id)")
    @Transactional
    public SavingsGoal transferFunds(Long id, SavingsFundTransferDTO request){
        //get target account
        Account account = accountRepository.findByUserIdAndNameAndActiveTrue(SecurityUtil.getUserId(), request.getAccountName())
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));
        
        //get savings goal
        SavingsGoal savingsGoal = savingsGoalRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Savings goal is missing"
            ));

        //get category for transaction
        Category category = categoryRepository.findByName("Fund Transfer").
                    orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "internal error"
            ));
        
        //withdrawing
        if(request.getType().equalsIgnoreCase("Withdraw")){
            //check for sufficient funds
            if (savingsGoal.getCurrentAmount().compareTo(request.getAmount()) >= 0) {
            //account deposit transaction
            String description = "Withdraw from savings";

            TransactionCreationRequest transaction = new TransactionCreationRequest(LocalDate.now(), request.getAmount(), category.getId(), account.getName(), "Deposit", description);
            
            transactionService.saveTransaction(transaction);
            savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().subtract(request.getAmount()));
        } else {
                throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "insufficient funds"
            );
            }
        }
        //depositing
        else if (request.getType().equalsIgnoreCase("Deposit")) {
            if (account.getAmount().compareTo(request.getAmount()) >= 0) {
            //account expense transaction
            String description = "Deposit to savings";
            
            TransactionCreationRequest transaction = new TransactionCreationRequest(LocalDate.now(), request.getAmount(), category.getId(), account.getName(), "Expense", description);
            
            transactionService.saveTransaction(transaction);
            savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().add(request.getAmount()));
            } else {
            throw new RuntimeException("Insufficient funds in account");
            }
        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Incorret transaction type"
            );
        }
        return savingsGoalRepository.save(savingsGoal);
    }
    
    //get net savings monthly balance
    public Map<String, BigDecimal> getNetMonthlyBalance(){
        //get user accounts
        List<Long> accountIds = accountUtil.getUserAccountIds();
        
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        
             
        for (int i = 9; i >= 0; i--) {

        LocalDate targetMonth = today.minusMonths(i);
        LocalDate startOfMonth = targetMonth.withDayOfMonth(1);
        LocalDate endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());
        String monthLabel = startOfMonth.getMonth().toString().substring(0, 3);
        
        //check if active month
        boolean hasTransactions = transactionRepository
            .existsByAccountIdInAndDateBetween(accountIds, startOfMonth, endOfMonth);
        if(hasTransactions){
            BigDecimal netBalance = transactionRepository.netSavingsMonthlyBalance(accountIds, startOfMonth, endOfMonth);
            breakdown.put(monthLabel, netBalance);
        }
        
        }
        return breakdown;
    }
    
    
    
}
