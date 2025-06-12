
package JK.pfm.repository;



import JK.pfm.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsDefaultTrue();
    Optional<Category> findByName(String name);
    
    //used to find IDs of system categories
    @Query("SELECT c.id FROM Category c WHERE c.name = :name")
    Optional<Long> findIdByName(String name);
    }

