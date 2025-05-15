
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author user
 */
public class CategoryNameDto {
    @NotBlank(message = "Category name is required")
    private String name;

    public CategoryNameDto() {}
    public CategoryNameDto(String name) { this.name = name; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
