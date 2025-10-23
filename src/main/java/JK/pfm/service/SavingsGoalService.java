package JK.pfm.service;

import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.dto.UpdateSavingsAmountDto;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.SavingsGoal;
import JK.pfm.model.User;
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

    /**
	 * Retrieves all savings goals for the specified user.
	 * <p>
	 * Applies a specification to ensure ownership by the given {@code userId}.
	 *
	 * @param userId the user ID whose savings goals to fetch
	 * @return a list of {@link JK.pfm.model.SavingsGoal} for the user
	 */
    public List<SavingsGoal> getAllSavingsGoals(Long userId) {
        Specification<SavingsGoal> spec = Specification.where(SavingsGoalSpecification.belongsToUser(userId));
        return savingsGoalRepository.findAll(spec);
    }
    
    /**
	 * Returns the total balance across all savings goals for the current user.
	 * <p>
	 * If no balance is recorded, returns {@code BigDecimal.ZERO}.
	 *
	 * @return the aggregated savings balance
	 */
    public BigDecimal getTotalBalance(){
        Long userId = SecurityUtil.getUserId();
        BigDecimal balance = savingsGoalRepository.getTotalBalanceByUserId(userId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    /**
	 * Creates a new savings goal for the current user.
	 * <p>
	 * Prevents duplicates by name (case-insensitive) per user.
	 *
	 * @param request the creation payload (name, target amount, description)
	 * @return the persisted {@link JK.pfm.model.SavingsGoal}
	 * @throws org.springframework.web.server.ResponseStatusException
	 *         if a savings goal with the same name already exists (409 CONFLICT)
	 */
    public SavingsGoal saveSavingsGoal(SavingGoalCreation request) {
        User user = SecurityUtil.getUser(userRepository);
        
        boolean already = savingsGoalRepository
        .existsByUserAndNameIgnoreCase(user, request.getName());
        if (already) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, 
                    "Savings goal with this name already exists"
            );
        }
        
        SavingsGoal goal = new SavingsGoal(
                request.getName(), 
                request.getTargetAmount(), 
                request.getDescription(), 
                user
                );
        return savingsGoalRepository.save(goal);
    }

    /**
	 * Retrieves a savings goal by its identifier.
	 *
	 * @param id the savings goal ID
	 * @return an {@link java.util.Optional} containing the {@link JK.pfm.model.SavingsGoal} if found
	 */
    public Optional<SavingsGoal> getSavingsGoalById(Long id) {
        return savingsGoalRepository.findById(id);
    }
    

    /**
	 * Deletes a savings goal.
	 * <p>
	 * Deletion is only allowed when the goal's current amount is zero.
	 *
	 * @param id the savings goal ID
	 * @throws org.springframework.web.server.ResponseStatusException
	 *         if the goal is not found (404 NOT FOUND) or if funds remain (409 CONFLICT)
	 */
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
    
    /**
	 * Updates the target amount of a savings goal.
	 *
	 * @param id the savings goal ID
	 * @param amount the payload containing the new target amount
	 * @return the updated {@link JK.pfm.model.SavingsGoal}
	 * @throws org.springframework.web.server.ResponseStatusException
	 *         if the goal is not found (404 NOT FOUND)
	 */
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
    
    /**
	 * Transfers funds between a savings goal and an account.
	 * <p>
	 * For {@code Withdraw}: moves funds from the savings goal to the account (creates an account
	 * {@code Deposit} transaction). For {@code Deposit}: moves funds from the account to the
	 * savings goal (creates an account {@code Expense} transaction). Uses the {@code "Fund Transfer"}
	 * category for transactions.
	 *
	 * @param id the savings goal ID
	 * @param request the transfer details (account name, amount, type: {@code Withdraw} or {@code Deposit})
	 * @return the updated {@link JK.pfm.model.SavingsGoal}
	 * @throws org.springframework.web.server.ResponseStatusException
	 *         if the account is not found (404 NOT FOUND),
	 *         if the savings goal is not found (404 NOT FOUND),
	 *         if the transfer category is missing (404 NOT FOUND),
	 *         if funds are insufficient in the source (409 CONFLICT), or
	 *         if the transfer type is invalid (400 BAD REQUEST)
	 */
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
    
    /**
	 * Computes the net monthly savings balance over the last 10 months.
	 * <p>
	 * Includes only months that have at least one transaction. Keys are 3-letter month labels
	 * (e.g., {@code "Jan"}).
	 *
	 * @return a map of month label → net savings balance for that month
	 */
    public Map<String, BigDecimal> getNetMonthlyBalance(){
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        List<Long> accountIds = accountUtil.getUserAccountIds();
        if(accountIds.isEmpty()){
            return breakdown;
        }
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
