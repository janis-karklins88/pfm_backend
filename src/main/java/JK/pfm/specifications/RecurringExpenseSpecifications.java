
package JK.pfm.specifications;

import JK.pfm.model.RecurringExpense;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;


public class RecurringExpenseSpecifications {
    public static Specification<RecurringExpense> belongsToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId);
    }
    
    // Filter transactions by date range
    public static Specification<RecurringExpense> dateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("startDate"), startDate, endDate);
    }

    public static Specification<RecurringExpense> dateGreaterThanOrEqual(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), startDate);
    }

    public static Specification<RecurringExpense> dateLessThanOrEqual(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), endDate);
    }

    // Filter by category id
    public static Specification<RecurringExpense> categoryEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    // Filter by account id
    public static Specification<RecurringExpense> accountEquals(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    
}
