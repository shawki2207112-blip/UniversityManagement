package edu.cse3230.ums.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Creates, validates and revokes (logout) JWTs. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    // Tokens that were logged out. In-memory is enough for a lab project.
    private final Set<String> revoked = ConcurrentHashMap.newKeySet();

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // secret must be >= 32 chars
        this.expirationMs = expirationMs;
    }

    public String generate(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Verifies signature and expiry; throws JwtException if the token is invalid or revoked. */
    public Claims parse(String token) {
        if (revoked.contains(token)) {
            throw new io.jsonwebtoken.JwtException("token revoked");
        }
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public void revoke(String token) {
        revoked.add(token);
    }
}
