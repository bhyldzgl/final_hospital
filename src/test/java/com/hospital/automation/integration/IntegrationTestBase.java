package com.hospital.automation.integration;

import com.hospital.automation.config.security.JwtTokenProvider;
import com.hospital.automation.domain.entity.Role;
import com.hospital.automation.domain.entity.User;
import com.hospital.automation.repository.RoleRepository;
import com.hospital.automation.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected JwtTokenProvider tokenProvider;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RoleRepository roleRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected String adminToken;
    protected String receptionistToken;
    protected String doctorToken;

    @BeforeEach
    void setupBaseUsers() {
        adminToken = jwtFor(uniqueUsername("admin"), Set.of("ROLE_ADMIN"));
        receptionistToken = jwtFor(uniqueUsername("recept"), Set.of("ROLE_RECEPTIONIST"));
        doctorToken = jwtFor(uniqueUsername("doc"), Set.of("ROLE_DOCTOR"));
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Patient.nationalId max=20 => biz 11 hane üretelim
    protected String uniqueNationalId11() {
        long base = 10000000000L; // 11 digit min
        long rnd = ThreadLocalRandom.current().nextLong(0, 89999999999L);
        long val = base + rnd;
        String s = String.valueOf(val);
        return s.length() > 11 ? s.substring(0, 11) : s;
    }

    protected String bearerTokenFor(String username, String... roles) {
        createUserWithRoles(username, username + "@test.local", "Passw0rd!", Set.of(roles));
        return tokenProvider.generateToken(username, List.of(roles));
    }

    /**
     * ✅ Geriye dönük uyumluluk: eski IT’ler jwtFor(username, Set<role>) çağırıyor.
     * Bu method token string döner (Bearer prefix YOK).
     */
    protected String jwtFor(String username, Set<String> roles) {
        createUserWithRoles(username, username + "@test.local", "Passw0rd!", roles);
        return tokenProvider.generateToken(username, new ArrayList<>(roles));
    }

    /**
     * ✅ Eski Integration testlerde kullanılan helper ile GERİYE DÖNÜK UYUMLULUK.
     */
    protected User createUserWithRoles(String username,
                                       String email,
                                       String rawPassword,
                                       Set<String> roleNames) {

        ensureRolesExist(roleNames);

        return userRepository.findByUsername(username).orElseGet(() -> {
            Set<Role> roles = roleNames.stream()
                    .map(r -> roleRepository.findByName(r).orElseThrow())
                    .collect(Collectors.toSet());

            User u = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .roles(roles)
                    .build();

            return userRepository.save(u);
        });
    }

    private void ensureRolesExist(Set<String> roles) {
        for (String r : roles) {
            roleRepository.findByName(r)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(r).build()));
        }
    }
}
