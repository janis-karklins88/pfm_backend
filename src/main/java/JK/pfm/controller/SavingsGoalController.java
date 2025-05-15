package JK.pfm.controller;

import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.UpdateSavingsAmountDto;
import JK.pfm.model.SavingsGoal;
import JK.pfm.service.SavingsGoalService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
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
        return ResponseEntity.ok(savingsGoalService.getAllSavingsGoals(SecurityUtil.getUserId()));
    }
    
    //get all savings balance
    @GetMapping("savings-balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        return ResponseEntity.ok(savingsGoalService.getTotalBalance());
    }

    //Create saving goal
    @PostMapping
    public ResponseEntity<SavingsGoal> createSavingsGoal(@Valid @RequestBody SavingGoalCreation request) {
        return ResponseEntity.ok(savingsGoalService.saveSavingsGoal(request));
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
    public ResponseEntity<SavingsGoal> updateSavingGoalAmount(@PathVariable Long id,@Valid @RequestBody UpdateSavingsAmountDto request){
        return ResponseEntity.ok(savingsGoalService.updateSavingsGoalAmount(id, request));
    }
    
    //add/remove saved ammount
    @PatchMapping("/{id}/transfer-funds")
    public ResponseEntity<SavingsGoal> transferFundsSavingsGoal(
            @PathVariable Long id, 
            @RequestBody SavingsFundTransferDTO request) {
        return ResponseEntity.ok(savingsGoalService.transferFunds(id, request));
    }
    
    //total net deposits
    @GetMapping("net-balance")
    public ResponseEntity<Map<String, BigDecimal>> getNetMonthlyBalance() {
        return ResponseEntity.ok(savingsGoalService.getNetMonthlyBalance());
    }
}
