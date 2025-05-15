// src/main/java/JK/pfm/dto/ChangeAccountNameDto.java
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangeAccountNameDto {

  @NotBlank(message = "Account name is required")
  private String name;

  public ChangeAccountNameDto() {}
  public ChangeAccountNameDto(String name) { this.name = name; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
