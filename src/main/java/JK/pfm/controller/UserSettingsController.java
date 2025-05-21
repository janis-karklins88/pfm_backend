
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
    
    @GetMapping
    public ResponseEntity<UserSettingsDto> getUserSettings(){
        UserSettingsDto dto = settingsService.getUserSettings();
        return ResponseEntity.ok(dto);
    }
    
    @PatchMapping("/currency")
    public ResponseEntity<Void> setUserCurrency(@RequestBody Map<String, String> request){
        String curr = request.get("currency");
        settingsService.setUserCurrency(curr);
        return ResponseEntity.noContent().build();
    }
}
