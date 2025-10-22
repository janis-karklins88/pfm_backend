package JK.pfm.dto.filters;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Filter used for retrieving transactions based on date range, category, account, or type.
 * <p>
 * Extends {@link DateRangeFilter} to include optional transaction-specific filters.
 */
public class TransactionFilter extends DateRangeFilter {
    
    /** 
     * Transaction type to filter by (optional).
     * Must be either {@code "Expense"} for spending or {@code "Deposit"} for incoming transactions.
     */
    @Pattern(regexp = "Expense|Deposit", message = "Unsupported transaction type")
    private String type;
    
    /** Category ID to filter by (optional). */
    private Long categoryId;
    
    /** Account ID to filter by (optional). */
    private Long accountId;
    
    public TransactionFilter() {
        super();
    }
    
      
    //getters setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

}
