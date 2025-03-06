package JK.pfm.service;

import JK.pfm.model.RecurringExpense;
import JK.pfm.repository.RecurringExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecurringExpenseService {

    @Autowired
    private RecurringExpenseRepository recurringExpenseRepository;
    
    // Get all recurring expenses
    public List<RecurringExpense> getAllRecurringExpenses() {
        return recurringExpenseRepository.findAll();
    }
    
    // Save or update a recurring expense
    public RecurringExpense saveRecurringExpense(RecurringExpense expense) {
        return recurringExpenseRepository.save(expense);
    }
    
    // Get a recurring expense by its ID
    public Optional<RecurringExpense> getRecurringExpenseById(Long id) {
        return recurringExpenseRepository.findById(id);
    }
    
    // Delete a recurring expense by ID
    public void deleteRecurringExpense(Long id) {
        recurringExpenseRepository.deleteById(id);
    }
}
