package JK.pfm.repository;

import JK.pfm.model.RecurringExpense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long>, JpaSpecificationExecutor<RecurringExpense> {
    List<RecurringExpense> findByNextDueDateLessThanEqualAndActiveTrue(LocalDate date);
    List<RecurringExpense> findTop5ByAccountIdInAndNextDueDateAfterAndActiveTrueOrderByNextDueDateAsc(List<Long> accountIds, LocalDate date);


}
