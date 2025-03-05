
package JK.pfm.util;


public class Validations {
 
    public static void emptyFieldValidation(String field, String fieldName){
        if(field==null || field.isEmpty()){
            throw new RuntimeException(fieldName +" cant be empty!");
        } 
    }
    
}
