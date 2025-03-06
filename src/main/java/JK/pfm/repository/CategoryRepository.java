
package JK.pfm.repository;



import JK.pfm.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // You can add custom query methods here if needed.
}

