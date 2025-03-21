
package JK.pfm.dto;


import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionCreationRequest {
    private LocalDate date;
    private BigDecimal amount;
    private String categoryName;
    private String accountName;
    private String type;
    private String description;
    
    // getters and setters
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String name) {
        this.categoryName = name;
    }
    
    public BigDecimal getAmount(){
        return amount;
    }
    
    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }
     public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String name) {
        this.accountName = name;
    }
    
    public void setDate(LocalDate date){
        this.date = date;
    }
    
    public LocalDate getDate(){
        return date;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}
