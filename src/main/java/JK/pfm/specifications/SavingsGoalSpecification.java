
package JK.pfm.specifications;

import JK.pfm.model.SavingsGoal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;


public class SavingsGoalSpecification {

    public static Specification<SavingsGoal> belongsToUser(Long userId) {
        return (Root<SavingsGoal> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
    

}
