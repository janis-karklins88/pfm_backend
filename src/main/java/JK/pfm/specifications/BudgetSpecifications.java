package JK.pfm.specifications;

import JK.pfm.model.Budget;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;


public class BudgetSpecifications {

    public static Specification<Budget> belongsToUser(Long userId) {
        return (Root<Budget> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
    
    // Specification to filter budgets that are active during a given period
    public static Specification<Budget> activeBetween(LocalDate filterStart, LocalDate filterEnd) {
        return (Root<Budget> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
            cb.and(
                cb.lessThanOrEqualTo(root.get("startDate"), filterEnd),
                cb.greaterThanOrEqualTo(root.get("endDate"), filterStart)
            );
    }
    
    /** Budgets whose startDate is on or after the filter’s startDate. */
    public static Specification<Budget> startDateOnOrAfter(LocalDate startDate) {
        return (root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("startDate"), startDate);
    }
    
    /** Budgets whose endDate is on or before the filter’s endDate. */
    public static Specification<Budget> endDateOnOrBefore(LocalDate endDate) {
        return (root, query, cb) ->
            cb.lessThanOrEqualTo(root.get("endDate"), endDate);
    }


    
}
