package JK.pfm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UnifiedTransactionDTO {
    private Long id;
    private LocalDate date; // For transactions, it's the transaction date; for recurring expenses its startDate.
    private BigDecimal amount;
    private String categoryName;
    private String accountName;
    private String type; // "Deposit", "Expense", or "Recurring Expense"
    private String description;
    // For recurring expenses 
    private String frequency; 



    // Constructors 
    public UnifiedTransactionDTO() {}

    public UnifiedTransactionDTO(Long id, LocalDate date, BigDecimal amount, String categoryName, 
                                 String accountName, String type, String description, String frequency) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.categoryName = categoryName;
        this.accountName = accountName;
        this.type = type;
        this.description = description;
        this.frequency = frequency;
    }

    // Getters and setters...
    public Long getId(){
        return id;
    }
    
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String name) {
        this.categoryName = name;
    }
    
    public BigDecimal getAmount(){
        return amount;
    }
    
    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }
     public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String name) {
        this.accountName = name;
    }
    
    public void setDate(LocalDate date){
        this.date = date;
    }
    
    public LocalDate getDate(){
        return date;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String freq) {
        this.frequency = freq;
    }

    public void setId(Long id) {
        this.id = id; 
    }
}
