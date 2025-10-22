
package JK.pfm.controller;

import JK.pfm.dto.UserSettingsDto;
import JK.pfm.service.UserSettingsService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/settings")
public class UserSettingsController {
    
    private final UserSettingsService settingsService;

    public UserSettingsController(UserSettingsService settingsService) {
        this.settingsService = settingsService;
    }
    
    	/**
	 * Retrieves the current user's application settings.
	 *
	 * <p>Responds with {@code 200 OK} and a {@link UserSettingsDto}
	 * containing the user's preferences such as default currency,
	 * theme, or other configurable values.</p>
	 *
	 * @return {@code ResponseEntity} containing the user's settings
	 * @implNote Delegates to {@link SettingsService#getUserSettings()}.
	 */
    @GetMapping
    public ResponseEntity<UserSettingsDto> getUserSettings(){
        UserSettingsDto dto = settingsService.getUserSettings();
        return ResponseEntity.ok(dto);
    }
    
    	/**
	 * Updates the preferred currency for the authenticated user.
	 *
	 * <p>Expects a JSON body with a {@code "currency"} key (e.g., {@code {"currency": "EUR"}}).
	 * Responds with {@code 204 No Content} if the update succeeds.</p>
	 *
	 * @param request a map containing the new currency under the key {@code "currency"}
	 * @return an empty {@code ResponseEntity} with {@code 204 No Content} status
	 * @implNote Delegates to {@link SettingsService#setUserCurrency(String)}.
	 */
    @PatchMapping("/currency")
    public ResponseEntity<Void> setUserCurrency(@RequestBody Map<String, String> request){
        String curr = request.get("currency");
        settingsService.setUserCurrency(curr);
        return ResponseEntity.noContent().build();
    }
}
