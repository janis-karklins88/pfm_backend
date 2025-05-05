
package JK.pfm.dto;


public class UserSettingsDto {
    private String currency;
    
    public UserSettingsDto(){}
    
    public UserSettingsDto(String curr){
        this.currency = curr;
    }
    
    public void setCurrency(String curr){
        this.currency = curr;  
        }
    
    public String getCurrency(){
        return currency;
    }
    
}
