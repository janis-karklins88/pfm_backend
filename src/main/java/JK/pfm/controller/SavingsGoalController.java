package JK.pfm.controller;

import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.SavingsFundTransferDTO;
import JK.pfm.dto.UpdateSavingsAmountDto;
import JK.pfm.model.SavingsGoal;
import JK.pfm.service.SavingsGoalService;
import JK.pfm.util.SecurityUtil;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;



@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;
    
    public SavingsGoalController (SavingsGoalService serv){
        this.savingsGoalService =serv;
    }

    //Get all saving goals
    @GetMapping
    public ResponseEntity<List<SavingsGoal>> getAllSavingsGoals() {
        return ResponseEntity.ok(savingsGoalService.getAllSavingsGoals(SecurityUtil.getUserId()));
    }
    
    //get all savings balance
    @GetMapping("/savings-balance")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        return ResponseEntity.ok(savingsGoalService.getTotalBalance());
    }

    //Create saving goal
    @PostMapping
    public ResponseEntity<SavingsGoal> createSavingsGoal(@Valid @RequestBody SavingGoalCreation request) {
        var savingGoal = savingsGoalService.saveSavingsGoal(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(savingGoal.getId())
        .toUri();
        return ResponseEntity.created(uri).body(savingGoal);
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSavingsGoal(@PathVariable Long id) {
        savingsGoalService.deleteSavingsGoal(id);
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
            @Valid @RequestBody SavingsFundTransferDTO request) {
        return ResponseEntity.ok(savingsGoalService.transferFunds(id, request));
    }
    
    //total net deposits
    @GetMapping("net-balance")
    public ResponseEntity<Map<String, BigDecimal>> getNetMonthlyBalance() {
        return ResponseEntity.ok(savingsGoalService.getNetMonthlyBalance());
    }
}
