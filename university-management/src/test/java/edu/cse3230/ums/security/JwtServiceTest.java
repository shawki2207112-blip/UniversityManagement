package edu.cse3230.ums.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UNIT TESTS 1-3 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-chars";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3_600_000L);
    }

    // Unit test 1
    @Test
    void generatedToken_canBeParsed_andContainsUsernameAndRole() {
        String token = jwtService.generate("admin", "ADMIN");

        Claims claims = jwtService.parse(token);

        assertEquals("teacher", claims.getSubject());
        assertEquals("TEACHER", claims.get("role", String.class));
        assertTrue(claims.getExpiration().after(new Date()), "token should not be expired yet");
    }

    // Unit test 2
    @Test
    void revokedToken_isRejected() {
        String token = jwtService.generate("student", "STUDENT");
        jwtService.revoke(token);

        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }

    // Unit test 3
    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        JwtService otherService = new JwtService("another-secret-key-also-at-least-32-chars!!", 3_600_000L);
        String forgedToken = otherService.generate("admin", "ADMIN");

        assertThrows(JwtException.class, () -> jwtService.parse(forgedToken));
    }
}
