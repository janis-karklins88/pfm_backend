
package JK.pfm.dto;

import java.math.BigDecimal;


public class ExpenseByCategoryDTO {
    private String categoryName;
    private BigDecimal totalAmount;

    public ExpenseByCategoryDTO(String categoryName, BigDecimal totalAmount) {
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
    }

    // Getters (and optionally setters)
    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
