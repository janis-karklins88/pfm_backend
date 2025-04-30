
package JK.pfm.dto;

import java.math.BigDecimal;


public class ExpenseByAccountDTO {
    private String accountName;
    private BigDecimal totalAmount;

    public ExpenseByAccountDTO(String accountName, BigDecimal totalAmount) {
        this.accountName = accountName;
        this.totalAmount = totalAmount;
    }

    // Getters (and optionally setters)
    public String getAccountName() {
        return accountName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
