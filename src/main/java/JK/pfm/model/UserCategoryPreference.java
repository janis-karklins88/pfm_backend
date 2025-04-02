package JK.pfm.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_category_preference", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "category_id"})
})
public class UserCategoryPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(name = "active")
    private Boolean active = true;

    // Constructors
    public UserCategoryPreference() {
    }

    public UserCategoryPreference(User user, Category category) {
        this.user = user;
        this.category = category;
        this.active = true;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public Boolean getActive(){
        return active;
    }
    
    public void setActive(Boolean active){
       this.active = active;
    }
}
