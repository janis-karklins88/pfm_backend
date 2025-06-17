
package JK.pfm.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateRecurringExpenseAccountDto {
    
    @NotNull (message = "Account ir required")
    private Long accountId;
    
    public UpdateRecurringExpenseAccountDto(){

    }
    
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
