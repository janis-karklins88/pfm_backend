package JK.pfm.service;

import JK.pfm.model.Account;
import JK.pfm.model.Transaction;
import JK.pfm.repository.TransactionRepository;
import JK.pfm.util.Validations;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    // Save a new transaction
    @Transactional
    public Transaction saveTransaction(Transaction transaction) {
    Account account = transaction.getAccount();
    
    //validations
    Validations.numberCheck(transaction.getAmount(), "Amount");
    Validations.negativeCheck(transaction.getAmount(), "Amount");
    Validations.checkDate(transaction.getDate());
    Validations.checkObj(account, "account");
    Validations.checkObj(transaction.getCategory(), "category");
    
    
    
    //transaction type validation
    if(!transaction.getType().equals("Deposit") && !transaction.getType().equals("Expense")){
            throw new RuntimeException("Incorrect transaction type!");
        }
    
    
    // Update the account balance based on the transaction type
    if (transaction.getType().equals("Expense")) {
        // check for sufficient funds
        if (account.getAmount().compareTo(transaction.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        account.setAmount(account.getAmount().subtract(transaction.getAmount()));
    } else if (transaction.getType().equals("Deposit")) {
        account.setAmount(account.getAmount().add(transaction.getAmount()));
    }
    
    return transactionRepository.save(transaction);
}
    
    // Get all transactions
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    // Get transaction by id
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }
    
    // Delete a transaction by id
    @Transactional
    public void deleteTransaction(Long id) {
        //retreiving transaction and account
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found!");
        }
        Transaction transaction = transactionOpt.get();
        Account account = transaction.getAccount();
        //updating account balance before delete
        if (transaction.getType().equals("Expense")){
            account.setAmount(account.getAmount().add(transaction.getAmount()));
        }
        else {
            //check for funds
            if (account.getAmount().compareTo(transaction.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
            account.setAmount(account.getAmount().subtract(transaction.getAmount()));
        }
        transactionRepository.deleteById(id);
    }
    
    
}

