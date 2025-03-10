package JK.pfm.repository;

import JK.pfm.model.RecurringExpense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    List<RecurringExpense> findByNextDueDateLessThanEqual(LocalDate date);

}
