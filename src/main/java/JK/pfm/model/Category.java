
package JK.pfm.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    //variables
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private Boolean active;
    
    // Constructors
    public Category() {
        this.active = true;
    }

    public Category(String name) {
        this.active = true;
        this.name = name;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
   
    public void setActive(Boolean set){
       this.active = set;
    }
    
    public Boolean getActive(){
        return active;
    }
}
