
package JK.pfm.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringExpenseCreation {
    @NotNull(message = "Starting is required")
    private LocalDate startDate;
    
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cant be negative")
    private BigDecimal amount;
    
    @NotNull(message = "Category missing")
    private Long categoryId;
    
    @NotBlank(message = "Account name is missing")
    private String accountName;
    
    @NotBlank(message = "Name is missing")
    private String name;
    
    @NotBlank(message = "Frequency is missing")
    @Pattern(
      regexp = "MONTHLY|WEEKLY|ANNUALLY",
      message = "Frequency must be one of: Monthly, Weekly, Annually"
    )
    private String frequency;
    
    public RecurringExpenseCreation (){}
    
    public RecurringExpenseCreation(LocalDate start, BigDecimal a, Long catId, String accName, String freq){
        this.startDate = start;
        this.amount = a;
        this.categoryId = catId;
        this.accountName = accName;
        this.frequency = freq;
    }

    
    // getters and setters
    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long id) {
        this.categoryId = id;
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
    
    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    
    
    
    
}
