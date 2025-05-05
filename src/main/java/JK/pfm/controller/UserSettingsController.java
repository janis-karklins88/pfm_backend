
package JK.pfm.controller;

import JK.pfm.dto.UserSettingsDto;
import JK.pfm.model.UserSettings;
import JK.pfm.repository.UserSettingsRepository;
import JK.pfm.service.UserSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/settings")
public class UserSettingsController {
    
    private final UserSettingsService settingsService;

    // Constructor-inject only the service
    public UserSettingsController(UserSettingsService settingsService) {
        this.settingsService = settingsService;
    }
    
    @GetMapping
    public ResponseEntity<UserSettingsDto> getUserCurrency(){
        UserSettingsDto dto = settingsService.getUserSettings();
        return ResponseEntity.ok(dto);
    }
}
