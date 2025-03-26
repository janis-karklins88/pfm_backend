
package JK.pfm.specifications;

import JK.pfm.model.RecurringExpense;
import org.springframework.data.jpa.domain.Specification;


public class RecurringExpenseSpecifications {
    public static Specification<RecurringExpense> belongsToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId);
    }
}
