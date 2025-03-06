package JK.pfm.repository;

import JK.pfm.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Optional: find transactions by date, category, etc.
    List<Transaction> findByCategoryId(Long categoryId);
    
}
