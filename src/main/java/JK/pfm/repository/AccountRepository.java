
package JK.pfm.repository;



import JK.pfm.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // You can add custom query methods here if needed.
    
}

