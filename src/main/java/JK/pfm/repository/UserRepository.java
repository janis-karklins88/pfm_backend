
package JK.pfm.repository;

import java.util.Optional;
import JK.pfm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
        

public interface UserRepository extends JpaRepository<User, Long> {
    	/**
	 * Finds a user by username.
	 *
	 * <p>Typically used during authentication to load user details.</p>
	 *
	 * @param username the username to look up
	 * @return an {@link Optional} containing the {@link User} if found, otherwise empty
	 */
    Optional<User> findByUsername(String username);
    
    	/**
	 * Checks whether a user with the specified username already exists.
	 *
	 * <p>Used for validation during user registration to prevent duplicates.</p>
	 *
	 * @param username the username to check
	 * @return {@code true} if a user with this username exists, {@code false} otherwise
	 */
    boolean existsByUsername(String username);
}

