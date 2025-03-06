package JK.pfm.service;

import JK.pfm.model.Budget;
import JK.pfm.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    //getting all budgets
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    //saving budget
    public Budget saveBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    //deleting budget
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }

    //getting budget by id
    public Optional<Budget> getBudgetById(Long id) {
        return budgetRepository.findById(id);
    }
}
