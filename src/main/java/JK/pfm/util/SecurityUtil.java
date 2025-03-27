package JK.pfm.util;

import JK.pfm.model.User;
import JK.pfm.repository.UserRepository;
import JK.pfm.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


public class SecurityUtil {


    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }


    public static User getUser(UserRepository userRepository) {
        Long userId = getUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
}
