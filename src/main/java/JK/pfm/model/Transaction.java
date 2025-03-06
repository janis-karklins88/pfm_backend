
package JK.pfm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    //Variables
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime date;
    
    private BigDecimal amount;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    // Constructors
    public Transaction() {
    }

    public Transaction(LocalDateTime date, BigDecimal amount, Account account, Category category) {
        this.date = date;
        this.amount = amount;
        this.account = account;
        this.category = category;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
