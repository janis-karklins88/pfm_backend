
package JK.pfm.dto;

import java.math.BigDecimal;


public class SavingGoalCreation {
    private BigDecimal targetAmount;
    private String name;
    private String description;
    

    
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
