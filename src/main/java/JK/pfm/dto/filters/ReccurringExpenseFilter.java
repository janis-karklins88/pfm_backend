/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JK.pfm.dto.filters;

/**
 * Filter used for retrieving recurring expenses based on date range, category, account.
 * <p>
 * Extends {@link DateRangeFilter} to include optional transaction-specific filters.
 */
public class ReccurringExpenseFilter extends DateRangeFilter {
    /** Category ID to filter by (optional). */
    private Long categoryId;
    /** Account ID to filter by (optional). */
    private Long accountId;
    
    public ReccurringExpenseFilter() {
        super();
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
