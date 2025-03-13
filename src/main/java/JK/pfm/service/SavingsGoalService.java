package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.model.SavingsGoal;
import JK.pfm.repository.SavingsGoalRepository;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SavingsGoalService {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    //getting all saving goals
    public List<SavingsGoal> getAllSavingsGoals() {
        return savingsGoalRepository.findAll();
    }

    //saving saving goals
    public SavingsGoal saveSavingsGoal(SavingsGoal goal) {
        return savingsGoalRepository.save(goal);
    }

    //getting saving goal
    public Optional<SavingsGoal> getSavingsGoalById(Long id) {
        return savingsGoalRepository.findById(id);
    }
    

    //deleting saving goal
    @Transactional
    public void deleteSavingsGoal(Long id) {
        Optional<SavingsGoal> savingsGoalOpt = savingsGoalRepository.findById(id);
        if (savingsGoalOpt.isEmpty()) {
           throw new RuntimeException("Savings goal not found!");
        }
        SavingsGoal savingsGoal = savingsGoalOpt.get();
    
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
        savingsGoalUpdate.setTargetAmount(amount);
        return savingsGoalRepository.save(savingsGoalUpdate);
    }
    
    //transfer funds
    @Transactional
    public SavingsGoal transferFunds(Long id, BigDecimal amount, String type, Account account){
        Validations.numberCheck(amount, "amount");
        Validations.negativeCheck(amount, "amount");
        Validations.checkObj(account, "acount");
        
        Optional <SavingsGoal> savingsGoalOpt = savingsGoalRepository.findById(id);
        if (savingsGoalOpt.isEmpty()) {
            throw new RuntimeException("Savings goal not found!");
        }
        SavingsGoal savingsGoal = savingsGoalOpt.get();
        //withdrawing
        if(type.equalsIgnoreCase("Withdraw")){
            //check for sufficient funds
            if (savingsGoal.getCurrentAmount().compareTo(amount) >= 0) {
            savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().subtract(amount));
            account.setAmount(account.getAmount().add(amount));
        } else {
                throw new RuntimeException("Insufficient funds");
            }
        }
        //depositing
        else {
            if (account.getAmount().compareTo(amount) >= 0) {
            savingsGoal.setCurrentAmount(savingsGoal.getCurrentAmount().add(amount));
            account.setAmount(account.getAmount().subtract(amount));
            } else {
            throw new RuntimeException("Insufficient funds in account");
            }
        }
        return savingsGoalRepository.save(savingsGoal);
    }
}
