package JK.pfm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget {

    //Variables
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private BigDecimal amount;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Boolean monthly = false;
    
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
    
    // Constructors
    public Budget() { }

    public Budget(BigDecimal amount, LocalDate startDate, LocalDate endDate, Category category, User user) {
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.category = category;
        this.user = user;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    
    public boolean getMonthly() {
        return monthly;
    }

    public void setMonthly(Boolean monthly) {
        this.monthly = monthly;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
    
    public User getUser(){
        return user;
    }
    
    public void setUser(User user){
        this.user = user;
    }
}
