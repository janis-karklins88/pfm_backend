package JK.pfm.specifications;

import JK.pfm.model.Budget;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable JPA {@link Specification} utilities for querying {@link Budget} entities.
 *
 * <p>These static methods are used to dynamically build filtering conditions
 * for repository queries, typically combined with
 * {@code Specification.where(...).and(...)} syntax.
 *
 * <p>All specifications are null-safe if used conditionally
 * (i.e., you can include them only when filter parameters are provided).
 *
 * <p>Example usage:
 * <pre>
 * Specification&lt;Budget&gt; spec = Specification.where(BudgetSpecifications.belongsToUser(userId))
 *     .and(BudgetSpecifications.activeBetween(start, end));
 * List&lt;Budget&gt; budgets = budgetRepository.findAll(spec);
 * </pre>
 */
public class BudgetSpecifications {

    /**
     * Matches budgets that belong to a specific user.
     *
     * @param userId the ID of the user who owns the budgets
     * @return specification filtering budgets by {@code user.id}
     */
    public static Specification<Budget> belongsToUser(Long userId) {
        return (Root<Budget> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
    
    /**
     * Matches budgets that are active during a specified date range.
     * <p>A budget is considered active if its {@code startDate} is before or equal to
     * the filter’s end date and its {@code endDate} is after or equal to the filter’s start date.</p>
     *
     * @param filterStart inclusive start date of the filter range
     * @param filterEnd   inclusive end date of the filter range
     * @return specification for overlapping active budgets
     */
    public static Specification<Budget> activeBetween(LocalDate filterStart, LocalDate filterEnd) {
        return (Root<Budget> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
            cb.and(
                cb.lessThanOrEqualTo(root.get("startDate"), filterEnd),
                cb.greaterThanOrEqualTo(root.get("endDate"), filterStart)
            );
    }
    
    /**
     * Matches budgets whose {@code startDate} is on or after the given date.
     *
     * @param startDate minimum start date (inclusive)
     * @return specification filtering budgets that start on or after {@code startDate}
     */
    public static Specification<Budget> startDateOnOrAfter(LocalDate startDate) {
        return (root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("startDate"), startDate);
    }
    
    /**
     * Matches budgets whose {@code endDate} is on or before the given date.
     *
     * @param endDate maximum end date (inclusive)
     * @return specification filtering budgets that end on or before {@code endDate}
     */
    public static Specification<Budget> endDateOnOrBefore(LocalDate endDate) {
        return (root, query, cb) ->
            cb.lessThanOrEqualTo(root.get("endDate"), endDate);
    }

}
