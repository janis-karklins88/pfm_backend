// src/main/java/JK/pfm/dto/ChangeUsernameDto.java
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangeUsernameDto {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 20,
        message = "Username must be between 3 and 20 characters")
  private String username;

  public ChangeUsernameDto() {}
  
  public ChangeUsernameDto(String username) { 
      this.username = username; 
  }

  public String getUsername() { 
      return username; 
  }
  public void setUsername(String username) { 
      this.username = username; 
  }
}
