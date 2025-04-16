package JK.pfm.controller;

import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.model.Account;

import JK.pfm.model.SavingsGoal;
import JK.pfm.model.User;
import JK.pfm.repository.AccountRepository;
import JK.pfm.repository.UserRepository;

import JK.pfm.service.SavingsGoalService;
import JK.pfm.util.SecurityUtil;
import JK.pfm.util.Validations;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    @Autowired
    private SavingsGoalService savingsGoalService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired 
    private UserRepository userRepository;
    
    

    //Get all saving goals
    @GetMapping
    public ResponseEntity<List<SavingsGoal>> getAllSavingsGoals() {
        //get user
        long userId = SecurityUtil.getUserId();
        List<SavingsGoal> goals = savingsGoalService.getAllSavingsGoals(userId);
        return ResponseEntity.ok(goals);
    }
    
    //get all savings balance
    @GetMapping("savings-balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        BigDecimal sum = savingsGoalService.getTotalBalance();
        return ResponseEntity.ok(sum);
    }

    //Create saving goal
    @PostMapping
    public ResponseEntity<SavingsGoal> createSavingsGoal(@RequestBody SavingGoalCreation request) {
        User user = SecurityUtil.getUser(userRepository);
        SavingsGoal goal = new SavingsGoal(request.getName(), request.getTargetAmount(), request.getCurrentAmount(), request.getDescription(), user);
        SavingsGoal savedGoal = savingsGoalService.saveSavingsGoal(goal);
        return ResponseEntity.ok(savedGoal);
    }

    //Get saving goal by id
    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoal> getSavingsGoalById(@PathVariable Long id) {
        return savingsGoalService.getSavingsGoalById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    //delete saving goal by id
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavingsGoal(@PathVariable Long id) {
        Long userId = SecurityUtil.getUserId();
        savingsGoalService.deleteSavingsGoal(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    //update saving goal
    @PatchMapping("/{id}/amount")
    public ResponseEntity<SavingsGoal> updateSavingGoalAmount(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request){
        BigDecimal newAmount = request.get("amount");
        Validations.numberCheck(newAmount, "amount");
        SavingsGoal updateSavingsGoal = savingsGoalService.updateSavingsGoalAmount(id, newAmount);
        return ResponseEntity.ok(updateSavingsGoal);
    }
    
    //add/remove saved ammount
     @PatchMapping("/{id}/transfer-funds")
    public ResponseEntity<SavingsGoal> transferFundsSavingsGoal(
            @PathVariable Long id, 
            @RequestBody SavingsFundTransferDTO request) {
        //get user id
        Long userId = SecurityUtil.getUserId();
        // Load the account
        Optional<Account> accOpt = accountRepository.findByUserIdAndName(userId, request.getAccountName());

        SavingsGoal updatedGoal = savingsGoalService.transferFunds(id, request.getAmount(), request.getType(), accOpt);
        return ResponseEntity.ok(updatedGoal);
    }
    
    //total net deposits
    @GetMapping("net-balance")
    public ResponseEntity<Map<String, BigDecimal>> getNetMonthlyBalance() {
        Map<String, BigDecimal> netBalance = savingsGoalService.getNetMonthlyBalance();
        return ResponseEntity.ok(netBalance);
    }
}
