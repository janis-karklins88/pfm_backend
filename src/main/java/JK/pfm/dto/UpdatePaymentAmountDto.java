/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JK.pfm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 *
 * @author user
 */
public class UpdatePaymentAmountDto {
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cant be negative")
    private BigDecimal amount;

    public UpdatePaymentAmountDto() {}
    public UpdatePaymentAmountDto(BigDecimal amnt) { this.amount = amnt; }

    public BigDecimal getAmount() { return amount; }
    public void setName(BigDecimal amnt) { this.amount = amnt; }
}
