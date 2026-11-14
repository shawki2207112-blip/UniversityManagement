package edu.cse3230.ums.controller;

import edu.cse3230.ums.model.AppUser;
import edu.cse3230.ums.model.Role;
import edu.cse3230.ums.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UNIT TESTS 4-6: UserController with a mocked repository (no Spring, no database). */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository repo;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(repo, encoder);
    }

    // Unit test 4
    @Test
    void create_hashesPassword_andTakesRoleFromUrlNotFromBody() {
        AppUser body = new AppUser();
        body.setUsername("s1");
        body.setPassword("plain123");
        body.setRole(Role.ADMIN);                     // a sneaky client tries to create an admin...

        when(repo.existsByUsername("s1")).thenReturn(false);
        when(repo.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<AppUser> response = controller.create("students", body);   // ...through /api/students

        assertEquals(201, response.getStatusCode().value());
        AppUser saved = response.getBody();
        assertEquals(Role.STUDENT, saved.getRole());              // role came from the URL
        assertNotEquals("plain123", saved.getPassword());         // not stored as plain text
        assertTrue(encoder.matches("plain123", saved.getPassword()));
    }

    // Unit test 5
    @Test
    void create_withExistingUsername_returns409_andSavesNothing() {
        AppUser body = new AppUser();
        body.setUsername("dup");
        body.setPassword("pass123");

        when(repo.existsByUsername("dup")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create("teachers", body));

        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).save(any(AppUser.class));
    }

    // Unit test 6
    @Test
    void get_unknownId_returns404() {
        when(repo.findByIdAndRole(99L, Role.TEACHER)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.get("teachers", 99L));

        assertEquals(404, ex.getStatusCode().value());
    }
}
