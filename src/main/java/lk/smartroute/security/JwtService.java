package lk.smartroute.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lk.smartroute.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
        @Value("${app.jwt.secret:${JWT_SECRET:SmartRouteLocalDevelopmentJwtSecretMustBeAtLeast32BytesLong123}}") String secret,
        @Value("${app.jwt.expiration-ms:${JWT_EXPIRATION_MS:86400000}}") long expirationMs
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(User user) {
        Date now = new Date();
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("uid", user.getId())
            .claim("role", user.getRole().name())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(key)
            .compact();
    }

    public String email(String token) {
        return claims(token).getSubject();
    }

    public Claims claims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
