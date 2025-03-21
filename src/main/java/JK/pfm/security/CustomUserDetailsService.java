package JK.pfm.security;

import JK.pfm.model.User; // Adjust with your actual User model package
import JK.pfm.repository.UserRepository; // Adjust accordingly
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch the user from the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Build and return a CustomUserDetails instance
        return new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword()); // Adjust getAuthorities() as needed
    }
}
