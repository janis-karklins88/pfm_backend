
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;


public class SavingGoalCreation {
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cant be negative")
    private BigDecimal targetAmount;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    public SavingGoalCreation(){
        
    }
    
    public SavingGoalCreation(BigDecimal target, String name, String desc){
        this.targetAmount = target;
        this.name = name;
        this.description = desc;
    }

    
    // getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    

    
    public BigDecimal getTargetAmount(){
        return targetAmount;
    }
    
    public void setTargetAmount(BigDecimal amount){
        this.targetAmount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}
