package JK.pfm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;


@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    //Variables
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private BigDecimal targetAmount;
    
    private BigDecimal currentAmount = BigDecimal.ZERO;
    
    private String description;
    
    private BigDecimal lastMonthAmount;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
    
    // Constructors
    public SavingsGoal() {
    }

    public SavingsGoal(String name, BigDecimal targetAmount, String description, User user) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.description = description;
        this.user = user;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id){
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }
    
    public BigDecimal getlastMonthAmount() {
        return lastMonthAmount;
    }

    public void setlastMonthAmount(BigDecimal lastMonthAmount) {
        this.lastMonthAmount = lastMonthAmount;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public User getUser(){
        return user;
    }
    
    public void setUser(User user){
        this.user = user;
    }
}
