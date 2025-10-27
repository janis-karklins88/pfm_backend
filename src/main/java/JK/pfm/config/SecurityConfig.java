package JK.pfm.config;

import JK.pfm.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for the API.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Registers a {@link PasswordEncoder} for hashing user passwords (BCrypt).</li>
 *   <li>Configures CORS to allow the frontend to call the API.</li>
 *   <li>Defines the security filter chain: disables CSRF for stateless JWT usage,
 *       permits a small set of public endpoints, and requires authentication elsewhere.</li>
 *   <li>Inserts a custom {@code JwtAuthenticationFilter} before
 *       {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}
 *       to authenticate requests by bearer token.</li>
 * </ul>
 *
 * <p><strong>Notes</strong>:
 * <ul>
 *   <li>CORS currently allows any origin via {@code setAllowedOriginPatterns("*")}. This is convenient
 *       in development but should be restricted to trusted origins in production.</li>
 *   <li>CSRF is disabled because the API is expected to be used with stateless JWTs (no browser session cookies).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter){
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    /**
     * Password encoder used for hashing user passwords at registration
     * and verifying credentials at login.
     *
     * <p>Uses BCrypt with a sensible default strength.</p>
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
    
    /**
     * CORS configuration for cross-origin requests from the frontend.
     *
     * <p>Current policy:
     * <ul>
     *   <li>Origins: <em>any</em> (via allowed origin patterns).</li>
     *   <li>Methods: GET, POST, PATCH, PUT, DELETE, OPTIONS.</li>
     *   <li>Headers: <em>any</em>.</li>
     *   <li>Credentials: allowed.</li>
     * </ul>
     *
     * <p><strong>Production tip:</strong> replace {@code "*"} with explicit frontend origins
     * (e.g., {@code https://app.example.com}).</p>
     *
     * @return a {@link CorsConfigurationSource} applied to all paths
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allowing all origins for now
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    /**
     * Main security filter chain.
     *
     * <p>Behavior:
     * <ul>
     *   <li><b>CSRF disabled</b> – suitable for token-based (JWT) APIs without cookies.</li>
     *   <li><b>CORS enabled</b> – uses {@link #corsConfigurationSource()}.</li>
     *   <li><b>Authorization</b> – permits {@code /api/users/register}, {@code /api/users/login},
     *       and {@code /api/health}; all other endpoints require authentication.</li>
     *   <li><b>JWT filter</b> – {@code JwtAuthenticationFilter} runs before the username/password filter
     *       to extract and validate bearer tokens.</li>
     * </ul>
     *
     * @param http the shared {@link HttpSecurity} builder
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/register", "/api/users/login", "/api/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
