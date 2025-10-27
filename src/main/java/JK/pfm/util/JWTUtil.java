package JK.pfm.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

/**
 * Utility component for generating, parsing, and validating JSON Web Tokens (JWTs)
 * used for stateless authentication.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Create signed JWTs that include the authenticated username as the subject.</li>
 *   <li>Extract the username (subject) from an existing token.</li>
 *   <li>Validate a token’s signature, structure, and expiration.</li>
 * </ul>
 *
 * <p>Tokens are signed using the HMAC-SHA256 algorithm and a shared secret key.
 * The expiration period is currently <b>24 hours</b>.</p>
 *
 * <p><strong>Security notice:</strong> The {@code SECRET_KEY} value should be replaced
 * with a secure, randomly generated secret and stored externally (e.g. in
 * environment variables or a configuration file) — never committed to source control.</p>
 */
@Component
public class JWTUtil {
    /** Secret key used for HMAC-SHA256 signing. Replace with a secure, environment-specific key. */
    private static final String SECRET_KEY = "yourSuperSecretKey1234567890123456"; 

    /**
     * Generates a signed JWT for the specified username.
     *
     * <p>The token includes:</p>
     * <ul>
     *   <li><b>Subject:</b> username</li>
     *   <li><b>IssuedAt:</b> current timestamp</li>
     *   <li><b>Expiration:</b> 24 hours from creation</li>
     * </ul>
     *
     * @param username the username to include as the subject
     * @return a compact JWT string suitable for inclusion in the {@code Authorization} header
     */
    public static String generateToken(String username) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1-day expiry
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the username (JWT subject) from a valid token.
     *
     * @param token the JWT string
     * @return the username embedded as the subject
     * @throws io.jsonwebtoken.JwtException if the token is invalid or malformed
     */
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validates the integrity and expiration of a JWT.
     *
     * <p>The token is parsed and verified using the shared secret key.
     * Returns {@code true} if signature and expiration are valid, otherwise {@code false}.</p>
     *
     * @param token the JWT string to validate
     * @return {@code true} if the token is valid; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(token);
            return true; // Valid token
        } catch (JwtException e) {
            return false; // Invalid token
        }
    }
}
