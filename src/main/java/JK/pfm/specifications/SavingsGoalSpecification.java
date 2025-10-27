
package JK.pfm.specifications;

import JK.pfm.model.SavingsGoal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable JPA {@link Specification} utilities for querying {@link SavingsGoal} entities.
 *
 * <p>Currently provides filters for restricting results to the authenticated user,
 * but can be extended later with goal name, target amount, or completion-status filters.</p>
 *
 * <p>Example usage:
 * <pre>
 * Specification&lt;SavingsGoal&gt; spec = Specification
 *     .where(SavingsGoalSpecification.belongsToUser(userId));
 *
 * List&lt;SavingsGoal&gt; goals = savingsGoalRepository.findAll(spec);
 * </pre>
 */
public class SavingsGoalSpecification {

    /**
     * Matches savings goals that belong to a specific user.
     *
     * @param userId ID of the user who owns the savings goal
     * @return specification filtering by {@code user.id}
     */
    public static Specification<SavingsGoal> belongsToUser(Long userId) {
        return (Root<SavingsGoal> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
    

}
