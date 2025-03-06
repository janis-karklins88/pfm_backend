package JK.pfm.repository;

import JK.pfm.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    // You can add methods like findByCategoryId, findByStartDateBetween, etc.
}
