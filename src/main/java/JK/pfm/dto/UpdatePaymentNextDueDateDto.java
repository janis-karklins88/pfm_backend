/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JK.pfm.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;


public class UpdatePaymentNextDueDateDto {
    @NotNull(message = "Start date is required")
    private LocalDate nextDueDate;
    
    public UpdatePaymentNextDueDateDto(LocalDate date) {
        this.nextDueDate = date;
    }
    
    public void setNextDueDate(LocalDate date){
        this.nextDueDate = date;
    }
    
    public LocalDate getNextDueDate(){
        return nextDueDate;
    }
}
