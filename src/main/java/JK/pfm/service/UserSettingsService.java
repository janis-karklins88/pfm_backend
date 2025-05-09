
package JK.pfm.service;

import JK.pfm.dto.UserSettingsDto;
import JK.pfm.model.UserSettings;
import JK.pfm.repository.UserSettingsRepository;
import JK.pfm.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {
    @Autowired
    public UserSettingsRepository userSettingsRepository;
    
    public UserSettingsDto getUserSettings(){
        Long userId = SecurityUtil.getUserId();
        UserSettings userSettings = userSettingsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User settings not found"));
        UserSettingsDto settings = new UserSettingsDto(userSettings.getCurrency());
        return settings;
    }
    
    public void setUserCurrency(String currency){
        Long userId = SecurityUtil.getUserId();
        UserSettings userSettings = userSettingsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User settings not found"));
        
        userSettings.setCurrency(currency);
        userSettingsRepository.save(userSettings);
    }
}
