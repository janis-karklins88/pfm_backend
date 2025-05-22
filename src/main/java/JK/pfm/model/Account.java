
package JK.pfm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    //Variables
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    @NotNull
    private Long version;
    
    private String name;
    
    private BigDecimal amount;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
    
    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private boolean active = true;

    
    // Constructors
    public Account() {
    }

    public Account(String name, BigDecimal amount, User user) {
        this.name = name;
        this.amount = amount;
        this.user = user;
    }
    
    //Getters & Setters
    public User getUser(){
        return user;
    }
    
    public void setUser(User user){
        this.user = user;
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
    
    public BigDecimal getAmount(){
        return amount;
    }
    
    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }
    
    public void setIsActive(Boolean set){
       this.active = set;
    }
    
    public Boolean getIsActive(){
        return active;
    }
    
    public Long getVersion() { 
        return version; 
    }
    public void setVersion(Long version) { 
        this.version = version; 
    }
}
