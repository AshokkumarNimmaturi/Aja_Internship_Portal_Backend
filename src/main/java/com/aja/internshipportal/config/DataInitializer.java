package com.aja.internshipportal.config;

import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        log.info("[STARTUP] Syncing User Statuses to OFFLINE...");
        
        // Find everyone who is currently NOT offline and reset them
        // This stops "Ghost Agents" from ringing when they haven't logged in.
        List<User> internalStaff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN || u.getRole() == User.Role.TUTOR)
                .toList();

        internalStaff.forEach(u -> {
            u.setStatus(User.SupportStatus.OFFLINE);
            u.setInCall(false);
            u.setAvailable(false); // Clean up legacy column too
        });

        userRepository.saveAll(internalStaff);
        log.info("[STARTUP] Reset {} staff members to OFFLINE.", internalStaff.size());
    }
}
