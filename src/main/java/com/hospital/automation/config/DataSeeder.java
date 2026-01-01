package com.hospital.automation.config;

import com.hospital.automation.domain.entity.Role;
import com.hospital.automation.domain.entity.User;
import com.hospital.automation.repository.RoleRepository;
import com.hospital.automation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.username}")
    private String adminUsername;

    @Value("${seed.admin.email}")
    private String adminEmail;

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1) Roles
        Role adminRole = ensureRole("ROLE_ADMIN");
        Role doctorRole = ensureRole("ROLE_DOCTOR");
        Role receptionRole = ensureRole("ROLE_RECEPTION");

        // 2) Admin user
        userRepository.findByUsername(adminUsername).ifPresentOrElse(
                u -> {
                    // admin zaten varsa dokunma
                },
                () -> {
                    User admin = User.builder()
                            .username(adminUsername)
                            .email(adminEmail)
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .roles(Set.of(adminRole))
                            .build();

                    userRepository.save(admin);
                }
        );
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }
}
