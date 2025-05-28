
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class AccountCreationRequest {
    @NotBlank(message = "Account name is required")
    private String name;
    
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cant be negative")
    private BigDecimal amount;
    
    public AccountCreationRequest(){
        
    }
    
    public AccountCreationRequest(String name, BigDecimal amount){
        this.name = name;
        this.amount = amount;
    }
    
    
    // getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getAmount(){
        return amount;
    }
    
    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }

}
