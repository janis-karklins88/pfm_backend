
package JK.pfm.dto;

import java.math.BigDecimal;

public class AccountCreationRequest {
    private String name;
    private BigDecimal amount;
    private String username;
    
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
     public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
