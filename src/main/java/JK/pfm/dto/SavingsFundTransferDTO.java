
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;


public class SavingsFundTransferDTO {
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cant be negative")
    private BigDecimal amount;
    @NotBlank(message = "Transfer type is required")
    private String type;
    @NotBlank(message = "Account name is required")
    private String accountName;
    
    // getters and setters
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
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
