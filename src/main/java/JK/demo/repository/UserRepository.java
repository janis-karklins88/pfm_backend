
package JK.demo.repository;

import java.util.Optional;
import JK.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
        

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}

