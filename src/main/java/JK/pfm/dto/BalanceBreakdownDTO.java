
package JK.pfm.dto;

import java.math.BigDecimal;


public class BalanceBreakdownDTO {
    private String name;
    private BigDecimal amount;
    
    public BalanceBreakdownDTO(){

    }
    
    public BalanceBreakdownDTO(String name, BigDecimal amount){
        this.name = name;
        this.amount = amount;
    }
    
    public String getName(){
        return name;
    }
    
   public void setName(String name){
       this.name = name;
   }
   
   public BigDecimal getAmount(){
        return amount;
    }
    
   public void setAmount(BigDecimal amount){
       this.amount = amount;
   }
}
