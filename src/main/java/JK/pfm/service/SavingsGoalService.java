package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.SavingsGoal;
import JK.pfm.model.Transaction;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.specifications.SavingsGoalSpecification;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

@Service
public class SavingsGoalService {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionService transactionService;

    //getting all saving goals
    public List<SavingsGoal> getAllSavingsGoals(Long userId) {
        Specification<SavingsGoal> spec = Specification.where(SavingsGoalSpecification.belongsToUser(userId));
        return savingsGoalRepository.findAll(spec);
    }

    //saving saving goals
    public SavingsGoal saveSavingsGoal(SavingsGoal goal) {
        Validations.emptyFieldValidation(goal.getName(), "Name");
        Validations.numberCheck(goal.getTargetAmount(), "Target amount");
        Validations.negativeCheck(goal.getTargetAmount(), "Target amount");
        Validations.numberCheck(goal.getCurrentAmount(), "Current amount");
        Validations.negativeCheck(goal.getCurrentAmount(), "Current amount");
        Validations.checkObj(goal.getUser(), "account");
        return savingsGoalRepository.save(goal);
    }

    //getting saving goal
    public Optional<SavingsGoal> getSavingsGoalById(Long id) {
        return savingsGoalRepository.findById(id);
    }
    

    //deleting saving goal
    @Transactional
    public void deleteSavingsGoal(Long id, Long userId) {
        Optional<SavingsGoal> savingsGoalOpt = savingsGoalRepository.findById(id);
        if (savingsGoalOpt.isEmpty()) {
           throw new RuntimeException("Savings goal not found!");
        }
        //check ownership
        SavingsGoal savingsGoal = savingsGoalOpt.get();
        if(!userId.equals(savingsGoal.getUser().getId())){
            throw new RuntimeException("Savings goal not found!");
        }
    
        // Check if there is any balance remaining
        if (savingsGoal.getCurrentAmount().compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("Savings goal still has funds. Please withdraw funds before deletion.");
        }
    
    savingsGoalRepository.deleteById(id);
}
    
    //update goal amount
    public SavingsGoal updateSavingsGoalAmount(Long id, BigDecimal amount) {
        Validations.numberCheck(amount, "amount");
        Validations.negativeCheck(amount, "amount");
        
        Optional <SavingsGoal> savingsGoalOpt = savingsGoalRepository.findById(id);
        if (savingsGoalOpt.isEmpty()) {
            throw new RuntimeException("Savings goal not found!");
        }
        SavingsGoal savingsGoalUpdate = savingsGoalOpt.get();
        //check ownership
        if(!savingsGoalUpdate.getUser().getId().equals(SecurityUtil.getUserId())){
            throw new RuntimeException("Savings goal not found!");
        }
        savingsGoalUpdate.setTargetAmount(amount);
        return savingsGoalRepository.save(savingsGoalUpdate);
    }
    
    //transfer funds
    @Transactional
    public SavingsGoal transferFunds(Long id, BigDecimal amount, String type, Optional<Account> accountOpt){
        Validations.numberCheck(amount, "amount");
        Validations.negativeCheck(amount, "amount");
        //account check
        if(accountOpt.isEmpty()){
            throw new RuntimeException("Account not correct");
        }
        Account account = accountOpt.get();
        
        
        Optional <SavingsGoal> savingsGoalOpt = savingsGoalRepository.findById(id);
        if (savingsGoalOpt.isEmpty()) {
            throw new RuntimeException("Savings goal not found!");
        }
        SavingsGoal savingsGoal = savingsGoalOpt.get();
        
        //withdrawing
        if(type.equalsIgnoreCase("Withdraw")){
            //check for sufficient funds
            if (savingsGoal.getCurrentAmount().compareTo(amount) >= 0) {
            //account deposit transaction
            String description = "Withdraw from savings";
            Optional<Category> catOpt = categoryRepository.findByName("Savings");
            Category category = catOpt.get();
            Transaction transaction = new Transaction(LocalDate.now(), amount, account, category, "Deposit", description);
            transactionService.saveTransaction(transaction);
            savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().subtract(amount));
        } else {
                throw new RuntimeException("Insufficient funds");
            }
        }
        //depositing
        else if (type.equalsIgnoreCase("Deposit")) {
            if (account.getAmount().compareTo(amount) >= 0) {
            //account expense transaction
            String description = "Deposit to savings";
            Optional<Category> catOpt = categoryRepository.findByName("Savings");
            Category category = catOpt.get();
            Transaction transaction = new Transaction(LocalDate.now(), amount, account, category, "Expense", description);
            transactionService.saveTransaction(transaction);
            savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().add(amount));
            } else {
            throw new RuntimeException("Insufficient funds in account");
            }
        } else {
            throw new RuntimeException("Incorrect type");
        }
        return savingsGoalRepository.save(savingsGoal);
    }
}
