
package JK.pfm.repository;



import JK.pfm.model.Category;
import JK.pfm.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsDefaultTrue();
    Optional<Category> findByName(String name);



}

