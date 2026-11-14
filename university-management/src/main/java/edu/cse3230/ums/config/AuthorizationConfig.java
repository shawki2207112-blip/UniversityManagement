package edu.cse3230.ums.config;

import edu.cse3230.ums.security.JwtAuthFilter;
import edu.cse3230.ums.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * AUTHORIZATION: "what are you allowed to do?"
 * Role-based access rules for every URL, plus the JWT filter that identifies the caller.
 */
@Configuration
public class AuthorizationConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
                                           UserDetailsService userDetailsService) throws Exception {
        http
            // No cookies/sessions are used (token in the Authorization header), so CSRF protection is not needed.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // public: the static pages and the login endpoint
                .requestMatchers("/", "/index.html", "/login.html", "/style.css", "/error").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                // Role rules (Admin = everything, Teacher = teachers, Student = students)
                .requestMatchers("/api/admins/**").hasRole("ADMIN")
                .requestMatchers("/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                .anyRequest().authenticated())

            // No token / bad token -> 401 (wrong role -> 403 automatically)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
