package JK.pfm.dto.filters;

import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Date range filter used across multiple queries.
 * <p>
 * Provides optional {@code startDate} and {@code endDate} fields that can be applied
 * to filter results within a specific period. If either field is {@code null},
 * the corresponding boundary is ignored.
 * <p>
 * Includes a built-in validation rule ensuring the start date is not after the end date.
 */
public class DateRangeFilter {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    /** Inclusive start date. (Optional) */
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    /** Inclusive end date. (Optional) */
    private LocalDate endDate;

        /**
	 * Validates that the date range is consistent.
	 *
	 * <p>This cross-field constraint ensures that if both {@code startDate} and {@code endDate}
	 * are provided, the start date is on or before the end date. If one or both fields are
	 * {@code null}, the validation passes.</p>
	 *
	 * @return {@code true} if the range is valid or incomplete, {@code false} if start is after end
	 */
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
