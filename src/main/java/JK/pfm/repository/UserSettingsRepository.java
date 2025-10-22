
package JK.pfm.repository;

import JK.pfm.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
	/**
	 * Retrieves the {@link UserSettings} entity associated with the specified user.
	 *
	 * <p>Each user is expected to have at most one settings record.</p>
	 *
	 * @param userId the ID of the user whose settings should be loaded
	 * @return the {@link UserSettings} for the given user, or {@code null} if not found
	 */    
    UserSettings findByUserId(Long userId);
}
