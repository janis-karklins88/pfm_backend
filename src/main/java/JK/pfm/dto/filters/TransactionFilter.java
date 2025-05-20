package JK.pfm.dto.filters;

import jakarta.validation.constraints.Pattern;


public class TransactionFilter extends DateRangeFilter {
    
    @Pattern(regexp = "Expense|Deposit", message = "Unsupported transaction type")
    private String type;
    
    private Long categoryId;
    
    private Long accountId;
    
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
