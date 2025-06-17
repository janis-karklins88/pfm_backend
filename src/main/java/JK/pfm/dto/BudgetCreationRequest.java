
package JK.pfm.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;


public class BudgetCreationRequest {
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cant be negative")
    private BigDecimal amount;
    
    @NotNull(message = "Category missing")
    private Long categoryId;

    
    @AssertTrue(message = "End date must be on or after start date")
    public boolean isEndDateOnOrAfterStart() {
    if (startDate == null || endDate == null) {
      return true;
    }
    
    return !endDate.isBefore(startDate);
  }
    
    public BudgetCreationRequest(){};
    
    public BudgetCreationRequest(BigDecimal amount, LocalDate start, LocalDate end, Long catId){
        this.amount = amount;
        this.startDate = start;
        this.endDate = end;
        this.categoryId = catId;
    }
    
    // getters and setters
    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long id) {
        this.categoryId = id;
    }
    
    public BigDecimal getAmount(){
        return amount;
    }
    
    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }
    
    public void setStartDate(LocalDate date){
        this.startDate = date;
    }
    
    public LocalDate getStartDate(){
        return startDate;
    }
    
    public void setEndDate(LocalDate date){
        this.endDate = date;
    }
    
    public LocalDate getEndDate(){
        return endDate;
    }
    
    
}
