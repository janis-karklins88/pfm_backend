package JK.pfm.model;
import jakarta.persistence.*;
import javax.validation.constraints.NotNull;


@Entity
@Table(name = "users")
public class User {
    
    //Varaibles
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @NotNull
    private String username;
    
    @Column(nullable = false)
    @NotNull
    private String password;
    
    //Constructor
    public User() {
    }
    
    public User(String username, String password){
        this.username = username;
        this.password = password;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
}
