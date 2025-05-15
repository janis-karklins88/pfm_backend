
package JK.pfm.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateRecurringExpenseAccountDto {
    
    @NotBlank (message = "Account ir required")
    private Long accountId;
    
    public UpdateRecurringExpenseAccountDto(Long id){
        this.accountId = id;
    }
    
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long id) {
        this.accountId = id;
    }
}
