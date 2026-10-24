package com.example.taskmanagement.config;

import com.example.taskmanagement.entity.Role;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.repository.RoleRepository;
import com.example.taskmanagement.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the three PRD roles plus one admin so the API is usable on a fresh database.
 * ponytail: fixed dev credentials, replace with an env-driven bootstrap when real deployment exists.
 */
@Configuration
@RequiredArgsConstructor
public class BootstrapConfig {

    static final List<String> ROLES = List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner seedData() {
        return args -> {
            ROLES.stream()
                    .filter(name -> roleRepository.findByName(name).isEmpty())
                    .forEach(name -> roleRepository.save(new Role(name)));
            if (!userRepository.existsByUsername("admin")) {
                var admin = new User("admin", "admin@example.com", passwordEncoder.encode("admin12345"));
                admin.getRoles().add(roleRepository.findByName("ROLE_ADMIN").orElseThrow());
                userRepository.save(admin);
            }
        };
    }
}
