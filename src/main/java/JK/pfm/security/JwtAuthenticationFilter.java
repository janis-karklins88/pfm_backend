package JK.pfm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import JK.pfm.util.JWTUtil;

/**
 * Servlet filter that authenticates requests using a JWT bearer token.
 *
 * <p>Flow:
 * <ol>
 *   <li>Skips authentication for public endpoints:
 *       {@code /api/users/register}, {@code /api/users/login}, and any path under {@code /api/health}.</li>
 *   <li>Reads the {@code Authorization} header and checks for the {@code Bearer <token>} scheme.</li>
 *   <li>Validates the token via {@link JWTUtil#validateToken(String)} and extracts the username.</li>
 *   <li>Loads {@link CustomUserDetails} and places an authenticated
 *       {@link UsernamePasswordAuthenticationToken} into the {@link SecurityContextHolder}.</li>
 *   <li>If validation fails or the header is absent, the filter does not authenticate the request;
 *       the chain continues and downstream authorization rules apply.</li>
 * </ol>
 *
 * <p><strong>Notes:</strong>
 * <ul>
 *   <li>Stateless: no session is used; credentials in the security token are set to {@code null}.</li>
 *   <li>Exception handling is intentionally lenient: any parsing/validation errors are swallowed so that
 *       the request proceeds as anonymous; protected endpoints will then return 401/403 later.</li>
 *   <li>Expected header format: {@code Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...}</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtil jwtUtil;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * Attempts JWT authentication for the current request and continues the filter chain.
     *
     * <p>Authentication is skipped for public endpoints. For other paths, if a valid bearer token
     * is present, an authenticated {@link UsernamePasswordAuthenticationToken} is created using
     * {@link CustomUserDetailsService#loadUserByUsername(String)} and stored in the security context.</p>
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param filterChain remaining filter chain
     * @throws ServletException on servlet errors
     * @throws IOException on I/O errors
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.equals("/api/users/register") || 
            path.equals("/api/users/login") ||
            path.startsWith("/api/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    
                    // Load the full user details
                    CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (Exception ex) {
                // Log or handle the exception as needed
            }
        }

        filterChain.doFilter(request, response);
    }
}
