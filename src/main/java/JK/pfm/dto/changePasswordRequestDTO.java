
package JK.pfm.dto;

/**
 *
 * @author user
 */
public class changePasswordRequestDTO {
    private String password;
    private String newPassword;
    private String newPasswordCheck;
    
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
