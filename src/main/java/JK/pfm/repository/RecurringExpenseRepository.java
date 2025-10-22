package JK.pfm.repository;

import JK.pfm.model.RecurringExpense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long>, JpaSpecificationExecutor<RecurringExpense> {
    	/**
	 * Finds all active recurring expenses that are due on or before the specified date.
	 *
	 * <p>Used to identify expenses that need to be processed or charged as of a given date.</p>
	 *
	 * @param date the cutoff date (inclusive)
	 * @return list of active {@link RecurringExpense} entries with a due date on or before the specified date
	 */
    List<RecurringExpense> findByNextDueDateLessThanEqualAndActiveTrue(LocalDate date);
    
	/**
	 * Retrieves the next five upcoming active recurring expenses for the specified accounts.
	 *
	 * <p>Results are ordered by {@code nextDueDate} ascending, showing the soonest payments first.</p>
	 *
	 * @param accountIds list of account IDs to search within
	 * @param date the reference date (only expenses due after this date are included)
	 * @return list of up to five upcoming {@link RecurringExpense} entries
	 */
    List<RecurringExpense> findTop5ByAccountIdInAndNextDueDateAfterAndActiveTrueOrderByNextDueDateAsc(List<Long> accountIds, LocalDate date);


}
