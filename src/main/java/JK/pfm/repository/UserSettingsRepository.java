
package JK.pfm.repository;

import JK.pfm.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    UserSettings findByUserId(Long userId);
}
