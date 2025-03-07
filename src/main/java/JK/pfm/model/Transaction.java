
package JK.pfm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;

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
    
    @NotNull(message = "Transaction type is required!")
    private String type;

    // Constructors
    public Transaction() {
    }

    public Transaction(LocalDateTime date, BigDecimal amount, Account account, Category category, String type) {
        this.date = date;
        this.amount = amount;
        this.account = account;
        this.category = category;
        this.type = type;
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
    
    public String getType(){
        return type;
    }
    
    public void setType(String type){
        this.type = type;
    }
}
