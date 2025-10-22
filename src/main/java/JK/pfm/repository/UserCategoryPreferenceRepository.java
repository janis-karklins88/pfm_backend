

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
    
	/**
	 * Retrieves all category preference records for a specific user.
	 *
	 * <p>Includes both active and inactive preferences.</p>
	 *
	 * @param userId the ID of the user
	 * @return list of {@link UserCategoryPreference} entities for the user
	 */
    List<UserCategoryPreference> findByUserId(Long userId);
    
	/**
	 * Checks whether a category preference exists for the given user and category.
	 *
	 * @param userId the user's ID
	 * @param categoryId the category's ID
	 * @return {@code true} if a record exists, otherwise {@code false}
	 */
    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);

	/**
	 * Retrieves a category preference for the given user and category combination.
	 *
	 * @param userId the user's ID
	 * @param categoryId the category's ID
	 * @return an {@link Optional} containing the preference if found
	 */
    Optional<UserCategoryPreference> findByUserIdAndCategoryId(Long userId, Long categoryId);

	/**
	 * Retrieves all active category preferences for a specific user.
	 *
	 * @param userId the user's ID
	 * @return list of active {@link UserCategoryPreference} records
	 */
    List<UserCategoryPreference> findByUserIdAndActiveTrue(Long userId);
    
	/**
	 * Checks if a user already has a category preference with the given category name (case-insensitive).
	 *
	 * <p>Used to prevent duplicate entries when users rename or re-add categories.</p>
	 *
	 * @param user the {@link User} entity
	 * @param categoryName the name of the category (case-insensitive)
	 * @return {@code true} if a matching preference exists, otherwise {@code false}
	 */    
    boolean existsByUserAndCategory_NameIgnoreCase(User user, String categoryName);
    
	/**
	 * Retrieves category data combined with user preference activity flags.
	 *
	 * <p>Returns a list of {@link CategoryListDto} entries containing
	 * category ID, name, and active status for the given user.</p>
	 *
	 * @param userId the ID of the user
	 * @return list of {@link CategoryListDto} projection results
	 */
    @Query(
      "SELECT new JK.pfm.dto.CategoryListDto(c.id, c.name, p.active) " +
      "FROM UserCategoryPreference p " +
      "JOIN p.category c " +
      "WHERE p.user.id = :userId"
    )
    List<CategoryListDto> findCategoryListDtoByUserId(@Param("userId") Long userId);
    
	/**
	 * Retrieves all active user category preferences for the specified user,
	 * eagerly fetching associated {@link Category} entities.
	 *
	 * <p>Used when active categories and their metadata are needed together
	 * (e.g., in settings or transaction filters).</p>
	 *
	 * @param userId the ID of the user
	 * @return list of active {@link UserCategoryPreference} records with joined categories
	 */
    @Query("SELECT p FROM UserCategoryPreference p JOIN FETCH p.category c WHERE p.user.id = :userId AND p.active = true")
    List<UserCategoryPreference> findActiveByUserWithCategory(@Param("userId") Long userId);


}
