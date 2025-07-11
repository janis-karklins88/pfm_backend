
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;


public class UserLoginRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    public UserLoginRequest(){

    }
    
    public UserLoginRequest(String un, String pass){
        this.password = pass;
        this.username = un;
    }
    
    public String getUsername(){
        return username;
    }
    
    public void setUsername(String username){
        this.username = username;
    }
    
    public String getPassword(){
        return password;
    }
    
    public void setPassword(String password){
        this.password = password;
    }
}
