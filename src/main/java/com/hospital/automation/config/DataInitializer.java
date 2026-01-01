package com.hospital.automation.config;

import com.hospital.automation.domain.entity.Role;
import com.hospital.automation.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;

    @Bean
    CommandLineRunner seedRoles() {
        return args -> {
            List<String> roles = List.of(
                    "ROLE_ADMIN",
                    "ROLE_DOCTOR",
                    "ROLE_NURSE",
                    "ROLE_RECEPTIONIST",
                    "ROLE_PATIENT"
            );

            for (String r : roles) {
                if (!roleRepository.existsByName(r)) {
                    roleRepository.save(Role.builder().name(r).build());
                }
            }
        };
    }
}
