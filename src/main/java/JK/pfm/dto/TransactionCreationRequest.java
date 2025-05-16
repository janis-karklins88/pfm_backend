
package JK.pfm.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionCreationRequest {
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cant be negative")
    private BigDecimal amount;
    
    @NotNull(message = "Category missing")
    private Long categoryId;
    
    @NotBlank(message = "Account missing")
    private String accountName;
    
    @NotBlank(message = "Type missing")
    @Pattern(
      regexp = "Expense|Deposit",
      message = "Unsupported transaction type"
    )
    private String type;
    
    private String description;
    
    
    
    //constructors
    public TransactionCreationRequest() { }
    
    public TransactionCreationRequest(LocalDate date, BigDecimal amount, Long categoryId, String accName, String type, String description){
        this.date = date;
        this.amount = amount;
        this.categoryId = categoryId;
        this.accountName = accName;
        this.type = type;
        this.description = description;
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
