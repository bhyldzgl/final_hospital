package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.config.security.JwtTokenProvider;
import com.hospital.automation.domain.dto.request.LoginRequest;
import com.hospital.automation.domain.dto.request.RegisterRequest;
import com.hospital.automation.domain.dto.response.AuthResponse;
import com.hospital.automation.domain.entity.Role;
import com.hospital.automation.domain.entity.User;
import com.hospital.automation.repository.RoleRepository;
import com.hospital.automation.repository.UserRepository;
import com.hospital.automation.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("username already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("email already exists");
        }

        Set<String> roleNames = normalizeRoles(request.roles());
        Set<Role> roles = new HashSet<>();
        for (String rn : roleNames) {
            Role role = roleRepository.findByName(rn)
                    .orElseThrow(() -> new BadRequestException("Role not found: " + rn));
            roles.add(role);
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(roles)
                .build();

        User saved = userRepository.save(user);

        String token = tokenProvider.generateToken(
                saved.getUsername(),
                saved.getRoles().stream().map(Role::getName).toList()
        );

        return new AuthResponse(
                token,
                "Bearer",
                saved.getId(),
                saved.getUsername(),
                saved.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet())
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // auth başarılıysa user çekelim
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = tokenProvider.generateToken(user.getUsername(), roles);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                new HashSet<>(roles)
        );
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        // Default: PATIENT
        if (roles == null || roles.isEmpty()) {
            return Set.of("ROLE_PATIENT");
        }

        Set<String> normalized = new HashSet<>();
        for (String r : roles) {
            if (r == null) continue;

            // Locale.ROOT: her işletim sisteminde aynı sonucu verir (TR-İ problemi çözülür)
            String x = r.trim().toUpperCase(Locale.ROOT);

            if (x.isBlank()) continue;

            if (!x.startsWith("ROLE_")) {
                x = "ROLE_" + x;
            }
            normalized.add(x);
        }

        if (normalized.isEmpty()) {
            normalized.add("ROLE_PATIENT");
        }
        return normalized;
    }
}
