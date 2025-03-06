package JK.pfm.controller;

import JK.pfm.model.SavingsGoal;
import JK.pfm.service.SavingsGoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
