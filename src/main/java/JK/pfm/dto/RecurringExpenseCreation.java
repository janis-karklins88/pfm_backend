
package JK.pfm.dto;


import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringExpenseCreation {
    private LocalDate startDate;
    private LocalDate nextDueDate;
    private BigDecimal amount;
    private String categoryName;
    private String accountName;
    private String name;
    private String frequency;

    
    // getters and setters
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String name) {
        this.categoryName = name;
    }
    
    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String name) {
        this.accountName = name;
    }
    
    public BigDecimal getAmount(){
        return amount;
    }
    
    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }
     public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setStartDate(LocalDate date){
        this.startDate = date;
    }
    
    public LocalDate getStartDate(){
        return startDate;
    }
    
    public void setNextDueDate(LocalDate date){
        this.nextDueDate = date;
    }
    
    public LocalDate getNextDueDate(){
        return nextDueDate;
    }
    
    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    
}
