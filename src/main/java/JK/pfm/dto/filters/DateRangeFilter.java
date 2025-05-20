package JK.pfm.dto.filters;

import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;


public class DateRangeFilter {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // Cross-field validation: start ≤ end
    @AssertTrue(message = "start date must be on or before end date")
    public boolean isValidRange() {
        if (startDate == null || endDate == null) return true;
        return !startDate.isAfter(endDate);
    }
    
    public DateRangeFilter() {}
    
    public DateRangeFilter(LocalDate start, LocalDate end){
        this.startDate = start;
        this.endDate = end;
    }

    //getters setters
    public LocalDate getStartDate(){
        return startDate;
    }
    
    public void setStartDate(LocalDate date){
        this.startDate = date;
    }
    
    public LocalDate getEndDate(){
        return endDate;
    }
    
    public void setEndDate(LocalDate date){
        this.endDate = date;
    }
}
