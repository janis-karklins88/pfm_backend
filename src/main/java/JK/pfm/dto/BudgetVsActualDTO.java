
package JK.pfm.dto;

import java.math.BigDecimal;

public class BudgetVsActualDTO {
    private String category;
    private BigDecimal budgeted;
    private BigDecimal actual;
    private BigDecimal difference;

    public BudgetVsActualDTO(String category, BigDecimal budgeted, BigDecimal actual) {
        this.category = category;
        this.budgeted = budgeted;
        this.actual = actual;
        this.difference = budgeted.subtract(actual);
    }

    // Getters and setters

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getBudgeted() {
        return budgeted;
    }

    public void setBudgeted(BigDecimal budgeted) {
        this.budgeted = budgeted;
    }

    public BigDecimal getActual() {
        return actual;
    }

    public void setActual(BigDecimal actual) {
        this.actual = actual;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }
}
