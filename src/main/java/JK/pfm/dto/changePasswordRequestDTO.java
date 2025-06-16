
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/**
 *
 * @author user
 */
public class changePasswordRequestDTO {
    @NotBlank(message = "Current password is required")
    private String password;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;
    
    @NotBlank(message = "Please confirm new password")
    private String newPasswordCheck;
    
    public changePasswordRequestDTO(){

    }
    
    public changePasswordRequestDTO(String pass1, String pass2, String pass3){
        this.password = pass1;
        this.newPassword = pass2;
        this.newPasswordCheck = pass3;
    }
    
    public String getPassword(){
        return password;
    }
    
    public void setPassword(String password){
        this.password = password;
    }
    
    public String getNewPassword(){
        return newPassword;
    }
    
    public void setNewPassword(String newPassword){
        this.newPassword = newPassword;
    }
    
    public String getNewPasswordCheck(){
        return newPasswordCheck;
    }
    
    public void setNewPasswordCheck(String newPasswordCheck){
        this.newPasswordCheck = newPasswordCheck;
    }
}
