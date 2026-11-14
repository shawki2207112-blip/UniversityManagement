package edu.cse3230.ums.config;

import edu.cse3230.ums.model.AppUser;
import edu.cse3230.ums.model.Role;
import edu.cse3230.ums.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates one demo account per role on first start so you can log in right away. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;
        create("admin", "admin123", "Demo Admin", Role.ADMIN);
        create("teacher", "teacher123", "Demo Teacher", Role.TEACHER);
        create("student", "student123", "Demo Student", Role.STUDENT);
    }

    private void create(String username, String rawPassword, String name, Role role) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(encoder.encode(rawPassword));
        u.setName(name);
        u.setEmail(username + "@university.edu");
        u.setRole(role);
        repo.save(u);
    }
}
