
package JK.pfm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;


public class BudgetCreationRequest {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private Long categoryId;
    
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
