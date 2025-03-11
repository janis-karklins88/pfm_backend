package JK.pfm.controller;

import JK.pfm.model.Budget;
import JK.pfm.model.SavingsGoal;
import JK.pfm.service.SavingsGoalService;
import JK.pfm.util.Validations;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    @Autowired
    private SavingsGoalService savingsGoalService;

    //Get all saving goals
    @GetMapping
    public ResponseEntity<List<SavingsGoal>> getAllSavingsGoals() {
        List<SavingsGoal> goals = savingsGoalService.getAllSavingsGoals();
        return ResponseEntity.ok(goals);
    }

    //Create saving goal
    @PostMapping
    public ResponseEntity<SavingsGoal> createSavingsGoal(@RequestBody SavingsGoal goal) {
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
        savingsGoalService.deleteSavingsGoal(id);
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
            @RequestBody Map<String, Object> request) {
        
        // Extract the amount from the request. Assuming the JSON contains a key "amount".
        BigDecimal amount;
        try {
            amount = new BigDecimal(request.get("amount").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        // Extract the operation type from the request. Expecting a key "type" with value "Deposit" or "Withdraw"
        String type = request.get("type").toString();
        if (!"Deposit".equalsIgnoreCase(type) && !"Withdraw".equalsIgnoreCase(type)) {
            return ResponseEntity.badRequest().build();
        }

        // Call the service layer to perform the transfer operation
        SavingsGoal updatedGoal = savingsGoalService.transferFunds(id, amount, type);
        return ResponseEntity.ok(updatedGoal);
    }
}
