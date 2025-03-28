
package JK.pfm.dto;

import java.math.BigDecimal;


public class SavingsFundTransferDTO {
    private BigDecimal amount;
    private String type;
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
