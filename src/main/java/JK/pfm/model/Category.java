
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
    
    // Constructors
    public Category() {
    }

    public Category(String name) {
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
}
