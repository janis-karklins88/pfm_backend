
package JK.pfm.repository;



import JK.pfm.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    	/**
	 * Retrieves all default categories.
	 *
	 * <p>Default categories are system-defined and available to all users by default.</p>
	 *
	 * @return list of {@link Category} entities where {@code isDefault = true}
	 */
    List<Category> findByIsDefaultTrue();
    
    Optional<Category> findByName(String name);
    
	/**
	 * Retrieves the ID of a category by its name.
	 *
	 * <p>Primarily used for system-level lookups where only the ID is required,
	 * such as seeding or linking default categories.</p>
	 *
	 * @param name the category name
	 * @return an {@link Optional} containing the category ID if found
	 */
    @Query("SELECT c.id FROM Category c WHERE c.name = :name")
    Optional<Long> findIdByName(String name);
    }

