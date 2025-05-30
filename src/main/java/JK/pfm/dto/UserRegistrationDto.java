
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegistrationDto {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3–20 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    public UserRegistrationDto(String name, String pass){
        this.password = pass;
        this.username = name;
    }
    
    public void setUsername(String name){
        this.username = name;
    }
    public String getUsername(){
        return username;
    }
    
    public void setPassword(String pass){
        this.password = pass;
    }
    public String getPassword(){
        return password;
    }
    
}
