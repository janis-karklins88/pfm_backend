package JK.pfm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;



public class DailyTrend {
    private LocalDate date;
    private BigDecimal totalExpense;
    private BigDecimal totalIncome;

    public DailyTrend(LocalDate date, BigDecimal totalExpense, BigDecimal totalIncome) {
        this.date = date;
        this.totalExpense = totalExpense;
        this.totalIncome = totalIncome;
    }

    // Getters and setters

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }
}
