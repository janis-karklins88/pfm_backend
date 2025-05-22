

package JK.pfm.repository;

import JK.pfm.dto.CategoryListDto;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface UserCategoryPreferenceRepository extends JpaRepository<UserCategoryPreference, Long> {
    List<UserCategoryPreference> findByUserId(Long userId);
    
    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);

    Optional<UserCategoryPreference> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<UserCategoryPreference> findByUserIdAndActiveTrue(Long userId);
    
    
    boolean existsByUserAndCategory_NameIgnoreCase(User user, String categoryName);
    
    @Query(
      "SELECT new JK.pfm.dto.CategoryListDto(c.id, c.name, p.active) " +
      "FROM UserCategoryPreference p " +
      "JOIN p.category c " +
      "WHERE p.user.id = :userId"
    )
    List<CategoryListDto> findCategoryListDtoByUserId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM UserCategoryPreference p JOIN FETCH p.category c WHERE p.user.id = :userId AND p.active = true")
    List<UserCategoryPreference> findActiveByUserWithCategory(@Param("userId") Long userId);


}
