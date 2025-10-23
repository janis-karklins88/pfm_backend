
package JK.pfm.service;

import JK.pfm.dto.UserSettingsDto;
import JK.pfm.model.UserSettings;
import JK.pfm.repository.UserSettingsRepository;
import JK.pfm.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    
    public UserSettingsService(UserSettingsRepository repo){
        this.userSettingsRepository = repo;
    }
    /**
    * Retrieves the settings of the currently authenticated user.
    * <p>
    * Returns the user's preferences, such as the selected currency, as a
    * {@link JK.pfm.dto.UserSettingsDto}.
    *
    * @return the {@link JK.pfm.dto.UserSettingsDto} containing the user's settings
    * @throws org.springframework.web.server.ResponseStatusException
    *         if no settings are found for the current user (404 NOT FOUND)
    */
    public UserSettingsDto getUserSettings(){
        Long userId = SecurityUtil.getUserId();
        UserSettings userSettings = userSettingsRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User settings not found"
            ));
        UserSettingsDto settings = new UserSettingsDto(userSettings.getCurrency());
        return settings;
    }
    
    /**
    * Updates the preferred currency for the currently authenticated user.
    * <p>
    * Loads the user's settings, updates the currency field, and saves the
    * changes in the database.
    *
    * @param currency the new currency code to set (e.g., "EUR", "USD")
    * @throws org.springframework.web.server.ResponseStatusException
    *         if the user's settings are not found (404 NOT FOUND)
    */
    public void setUserCurrency(String currency){
        Long userId = SecurityUtil.getUserId();
        UserSettings userSettings = userSettingsRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User settings not found"
            ));
        
        userSettings.setCurrency(currency);
        userSettingsRepository.save(userSettings);
    }
}
