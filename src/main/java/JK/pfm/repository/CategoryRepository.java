
package JK.pfm.repository;



import JK.pfm.dto.CategoryListDto;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsDefaultTrue();
    Optional<Category> findByName(String name);
    Optional<Long> findIdByName(String name);
    
    



}

