
package JK.pfm.util;

import java.math.BigDecimal;


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
   

    
}
