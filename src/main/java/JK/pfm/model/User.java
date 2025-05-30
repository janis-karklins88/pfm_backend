package JK.pfm.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;



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
    
    // A user can have many category preferences
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserCategoryPreference> categoryPreferences = new HashSet<>();
    
    
    
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
    
    public Set<UserCategoryPreference> getCategoryPreferences() {
        return categoryPreferences;
    }
    
    public void setCategoryPreferences(Set<UserCategoryPreference> categoryPreferences) {
        this.categoryPreferences = categoryPreferences;
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
    
    public void addCategoryPreference(UserCategoryPreference preference) {
        categoryPreferences.add(preference);
        preference.setUser(this);
    }
    
    public void removeCategoryPreference(UserCategoryPreference preference) {
        categoryPreferences.remove(preference);
        preference.setUser(null);
    }
    
}
