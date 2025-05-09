

package JK.pfm.repository;

import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserCategoryPreferenceRepository extends JpaRepository<UserCategoryPreference, Long> {
    List<UserCategoryPreference> findByUserId(Long userId);
    
    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);

    Optional<UserCategoryPreference> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<UserCategoryPreference> findByUserIdAndActiveTrue(Long userId);
    
    
    boolean existsByUserAndCategory_NameIgnoreCase(User user, String categoryName);


}
