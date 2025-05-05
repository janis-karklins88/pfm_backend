

package JK.pfm.model;

import jakarta.persistence.*;


@Entity
@Table(name = "user_settings")
public class UserSettings {
    
    @Id
    @Column(name = "user_settings")
    private Long userId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "EUR";
    
    // Constructors
    public UserSettings() {}

    public UserSettings(User user, String currency) {
        this.user = user;
        this.currency = currency;
    }

    // Getters & Setters
    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
