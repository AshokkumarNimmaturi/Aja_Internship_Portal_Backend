package com.aja.internshipportal.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    // CommandLineRunner runs automatically when app starts
    // only seeds if admin does not already exist
    @Bean
    public CommandLineRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // check if admin already exists — skip if yes
            if (userRepository.existsByEmail("admin@aja.com")) {
                log.info("Admin already exists — skipping seed");
                return;
            }

            // create admin with hashed password
            User admin = User.builder()
                    .fullName("Super Admin")
                    .email("admin@aja.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .firstLogin(false)
                    .build();

            userRepository.save(admin);

            // print to console so you can see it ran
            log.info("✅ Admin seeded → email: admin@aja.com / password: admin123");
            log.info("⚠️  Change this password immediately after first login");
        };
    }
}