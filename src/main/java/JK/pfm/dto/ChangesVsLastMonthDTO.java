
package JK.pfm.dto;

import java.math.BigDecimal;


public class ChangesVsLastMonthDTO {
    private String name;
    private BigDecimal percentage;
    
    public ChangesVsLastMonthDTO() {
        }
    
    public ChangesVsLastMonthDTO(String name, BigDecimal percentageChange) {
            this.name = name;
            this.percentage = percentageChange;
        }
    
    public void setName(String name){
        this.name = name;
    }
    public String getName(){
        return name;
    }
    
    public void setPercentage(BigDecimal percentage){
        this.percentage = percentage;
    }
    public BigDecimal getPercentage(){
        return percentage;
    }
    
}
