package JK.pfm.service;

import JK.pfm.model.SavingsGoal;
import JK.pfm.repository.SavingsGoalRepository;
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
    public void deleteSavingsGoal(Long id) {
        savingsGoalRepository.deleteById(id);
    }
}
