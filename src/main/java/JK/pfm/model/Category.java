
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
    
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;
    
    // Constructors
    public Category() {
        this.isDefault = true;
    }

    public Category(String name) {
        this.isDefault = false;
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
    
   
    public void setIsDefault(Boolean set){
       this.isDefault = set;
    }
    
    public Boolean getIsDefault(){
        return isDefault;
    }
}
