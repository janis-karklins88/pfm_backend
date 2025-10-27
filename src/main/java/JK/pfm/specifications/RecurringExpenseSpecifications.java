
package JK.pfm.specifications;

import JK.pfm.model.RecurringExpense;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable JPA {@link Specification} utilities for querying {@link RecurringExpense} entities.
 *
 * <p>These specifications provide flexible, composable filters for dynamic queries,
 * typically used in services to fetch recurring expenses by user, account, category, or date range.</p>
 *
 * <p>All filters are compatible with {@code Specification.where(...).and(...)} chaining and
 * can safely be applied conditionally based on user input.</p>
 *
 * <p>Example usage:
 * <pre>
 * Specification&lt;RecurringExpense&gt; spec = Specification
 *     .where(RecurringExpenseSpecifications.belongsToUser(userId))
 *     .and(RecurringExpenseSpecifications.dateBetween(start, end))
 *     .and(RecurringExpenseSpecifications.categoryEquals(categoryId));
 *
 * List&lt;RecurringExpense&gt; expenses = recurringExpenseRepository.findAll(spec);
 * </pre>
 */
public class RecurringExpenseSpecifications {
    /**
     * Matches recurring expenses that belong to a specific user.
     * <p>The filter is applied through the linked account’s {@code user.id} field.</p>
     *
     * @param userId ID of the user who owns the account
     * @return specification limiting results to recurring expenses of that user
     */
    public static Specification<RecurringExpense> belongsToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId);
    }
    
    /**
     * Matches recurring expenses whose {@code nextDueDate} falls between two dates (inclusive).
     *
     * @param startDate start date of the filter range
     * @param endDate   end date of the filter range
     * @return specification filtering by {@code nextDueDate} between the provided dates
     */
    public static Specification<RecurringExpense> dateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("nextDueDate"), startDate, endDate);
    }

    /**
     * Matches recurring expenses with {@code nextDueDate} on or after the specified date.
     *
     * @param startDate inclusive lower bound for next due date
     * @return specification filtering by {@code nextDueDate} greater than or equal to {@code startDate}
     */
    public static Specification<RecurringExpense> dateGreaterThanOrEqual(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("nextDueDate"), startDate);
    }

    /**
     * Matches recurring expenses with {@code nextDueDate} on or before the specified date.
     *
     * @param endDate inclusive upper bound for next due date
     * @return specification filtering by {@code nextDueDate} less than or equal to {@code endDate}
     */
    public static Specification<RecurringExpense> dateLessThanOrEqual(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("nextDueDate"), endDate);
    }

    /**
     * Matches recurring expenses belonging to a specific category.
     *
     * @param categoryId ID of the category
     * @return specification filtering by {@code category.id}
     */
    public static Specification<RecurringExpense> categoryEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * Matches recurring expenses linked to a specific account.
     *
     * @param accountId ID of the account
     * @return specification filtering by {@code account.id}
     */
    public static Specification<RecurringExpense> accountEquals(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    
}
