
package JK.pfm.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;


public class Validations {
 
    //empty field validation
    public static void emptyFieldValidation(String field, String fieldName){
        if(field==null || field.trim().isEmpty()){
            throw new RuntimeException(fieldName +" cannot be empty!");
        } 
    }
    
    //negative number check
    public static void negativeCheck(BigDecimal number, String fieldName){
        if(number.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(fieldName + " cannot be negative!");
        }
    }
    
    //empty number check
    public static void numberCheck(BigDecimal number, String fieldName ){
        if(number == null) {
            throw new RuntimeException(fieldName + " cannot be empty!");
        }
    }
    
    //date field validation
    public static void checkDate(TemporalAccessor date) {
        if (date == null) {
            throw new IllegalArgumentException("Date must be provided!");
        }
    }
    
    //null validation
    public static void checkObj(Object object, String objName){
        if(object == null){
            throw new IllegalArgumentException(objName + " not provided!");
        }
    }
   
    //date validation, end date is after start date
    public static void checkStartEndDate(LocalDate start, LocalDate end){
        if(start != null && end != null && start.isAfter(end)){
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }

    
}
