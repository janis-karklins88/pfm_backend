package JK.pfm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_expenses")
public class RecurringExpense {

    //Variables
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;  
    
    private BigDecimal amount;
    
    private LocalDate startDate;
    
    private String frequency;
    
    private LocalDate nextDueDate;
    
    private LocalDate lastPayment = null;
    
    private Boolean active;
    
    
    @ManyToOne
    private Account account;
    
    @ManyToOne
    private Category category;
    
    
    // Constructors
    public RecurringExpense() {}

    public RecurringExpense(String name, BigDecimal amount, LocalDate startDate, String frequency, Account account, Category category) {
        this.name = name;
        this.amount = amount;
        this.startDate = startDate;
        this.frequency = frequency;
        this.account = account;
        this.category = category;
        this.nextDueDate = startDate;
        this.active = true;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id){
        this.id = id;
    }
    
    public Boolean getActive(){
        return active;
    }
    
    public void setActive(Boolean active){
        this.active = active;
    }

    public String getName() {
        return name;
    }
  
    public void setName(String name) {
        this.name = name;
    }
  
    public BigDecimal getAmount() {
        return amount;
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
    
    public LocalDate getLastPayment() {
        return lastPayment;
    }
  
    public void setLastPayment(LocalDate startDate) {
        this.lastPayment = startDate;
    }
  
    public String getFrequency() {
        return frequency;
    }
  
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
  
    public LocalDate getNextDueDate() {
        return nextDueDate;
    }
  
    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }
    
    public Account getAccount(){
        return account;
    }
    
    public void setAccount(Account account){
        this.account = account;
    }
    
    public Category getCategory(){
        return category;
    }
    
    public void setCategory(Category category){
        this.category = category;
    }
    
    }
