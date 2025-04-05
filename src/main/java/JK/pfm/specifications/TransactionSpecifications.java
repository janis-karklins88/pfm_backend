package JK.pfm.specifications;

import JK.pfm.model.Transaction;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class TransactionSpecifications {

    // Filter transactions by date range
    public static Specification<Transaction> dateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("date"), startDate, endDate);
    }

    public static Specification<Transaction> dateGreaterThanOrEqual(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), startDate);
    }

    public static Specification<Transaction> dateLessThanOrEqual(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), endDate);
    }

    // Filter by category id
    public static Specification<Transaction> categoryEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    // Filter by account id
    public static Specification<Transaction> accountEquals(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    // Filter by user id (through account)
    public static Specification<Transaction> belongsToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId);
    }
    
    //filter by type
    public static Specification<Transaction> typeEquals(String type){
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }
}
