package JK.pfm.specifications;

import JK.pfm.model.Transaction;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

/**
 * Reusable JPA {@link Specification} utilities for querying {@link Transaction} entities.
 *
 * <p>These static methods allow building flexible, composable filters
 * for dynamic transaction queries. Specifications are typically combined
 * using {@code Specification.where(...).and(...)} in service or repository layers.</p>
 *
 * <p>Each specification corresponds to a common filter condition such as date range,
 * category, account, user ownership, or transaction type.</p>
 *
 * <p>Example usage:
 * <pre>
 * Specification&lt;Transaction&gt; spec = Specification
 *     .where(TransactionSpecifications.belongsToUser(userId))
 *     .and(TransactionSpecifications.dateBetween(start, end))
 *     .and(TransactionSpecifications.categoryEquals(categoryId))
 *     .and(TransactionSpecifications.typeEquals("Expense"));
 *
 * List&lt;Transaction&gt; results = transactionRepository.findAll(spec);
 * </pre>
 */
public class TransactionSpecifications {

    /**
     * Matches transactions whose {@code date} falls between two dates (inclusive).
     *
     * @param startDate the start date of the filter range
     * @param endDate   the end date of the filter range
     * @return specification filtering transactions within the specified date range
     */
    public static Specification<Transaction> dateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> cb.between(root.get("date"), startDate, endDate);
    }

    /**
     * Matches transactions with {@code date} on or after the specified date.
     *
     * @param startDate inclusive lower bound for transaction date
     * @return specification filtering transactions on or after {@code startDate}
     */
    public static Specification<Transaction> dateGreaterThanOrEqual(LocalDate startDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), startDate);
    }

    /**
     * Matches transactions with {@code date} on or before the specified date.
     *
     * @param endDate inclusive upper bound for transaction date
     * @return specification filtering transactions on or before {@code endDate}
     */
    public static Specification<Transaction> dateLessThanOrEqual(LocalDate endDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), endDate);
    }

    /**
     * Matches transactions belonging to a specific category.
     *
     * @param categoryId ID of the category
     * @return specification filtering by {@code category.id}
     */
    public static Specification<Transaction> categoryEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * Matches transactions linked to a specific account.
     *
     * @param accountId ID of the account
     * @return specification filtering by {@code account.id}
     */
    public static Specification<Transaction> accountEquals(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    /**
     * Restricts transactions to those owned by a specific user.
     * <p>The filter is applied through the related account’s {@code user.id} field.</p>
     *
     * @param userId ID of the user who owns the account
     * @return specification limiting results to transactions of that user
     */
    public static Specification<Transaction> belongsToUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId);
    }
    
    /**
     * Matches transactions of a given type (e.g., "Expense", "Deposit").
     *
     * @param type transaction type string
     * @return specification filtering by {@code type} field
     */
    public static Specification<Transaction> typeEquals(String type){
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }
}
