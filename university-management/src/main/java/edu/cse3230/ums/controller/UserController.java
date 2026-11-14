package edu.cse3230.ums.controller;

import edu.cse3230.ums.model.AppUser;
import edu.cse3230.ums.model.Role;
import edu.cse3230.ums.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * CRUD for all three resources:
 *   /api/admins    /api/teachers    /api/students
 * Who may call which one is decided in AuthorizationConfig, not here.
 */
@RestController
public class UserController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserController(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    // ---- who am I? (used by the frontend) ----
    @GetMapping("/api/me")
    public Map<String, String> me(Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return Map.of("username", auth.getName(), "role", role);
    }

    // ---- CREATE ----
    @PostMapping("/api/{type:admins|teachers|students}")
    public ResponseEntity<AppUser> create(@PathVariable String type, @RequestBody AppUser body) {
        if (isBlank(body.getUsername()) || isBlank(body.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        if (repo.existsByUsername(body.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }
        body.setId(null);
        body.setRole(roleOf(type));                       // role comes from the URL, never from the request body
        body.setPassword(encoder.encode(body.getPassword()));
        AppUser saved = repo.save(body);
        return ResponseEntity.created(URI.create("/api/" + type + "/" + saved.getId())).body(saved);
    }

    // ---- READ ----
    @GetMapping("/api/{type:admins|teachers|students}")
    public List<AppUser> list(@PathVariable String type) {
        return repo.findByRole(roleOf(type));
    }

    @GetMapping("/api/{type:admins|teachers|students}/{id}")
    public AppUser get(@PathVariable String type, @PathVariable Long id) {
        return find(type, id);
    }

    // ---- UPDATE (username stays fixed; password only changes if one is sent) ----
    @PutMapping("/api/{type:admins|teachers|students}/{id}")
    public AppUser update(@PathVariable String type, @PathVariable Long id, @RequestBody AppUser body) {
        AppUser user = find(type, id);
        user.setName(body.getName());
        user.setEmail(body.getEmail());
        if (!isBlank(body.getPassword())) {
            user.setPassword(encoder.encode(body.getPassword()));
        }
        return repo.save(user);
    }

    // ---- DELETE ----
    @DeleteMapping("/api/{type:admins|teachers|students}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String type, @PathVariable Long id) {
        repo.delete(find(type, id));
    }

    // ---- helpers ----
    private AppUser find(String type, Long id) {
        return repo.findByIdAndRole(id, roleOf(type))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
    }

    private static Role roleOf(String type) {
        return switch (type) {
            case "admins" -> Role.ADMIN;
            case "teachers" -> Role.TEACHER;
            default -> Role.STUDENT;
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
