package com.hospital.automation.controller;

import com.hospital.automation.config.security.UserPrincipal;
import com.hospital.automation.domain.dto.request.LoginRequest;
import com.hospital.automation.domain.dto.request.RegisterRequest;
import com.hospital.automation.domain.dto.response.AuthResponse;
import com.hospital.automation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Token ile giriş yapan kişinin bilgisi
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AuthResponse me(Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Set<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        // /me endpoint’i token üretmez; token alanını boş döndürüyoruz
        return new AuthResponse(
                "",
                "Bearer",
                principal.getId(),
                principal.getUsername(),
                roles
        );
    }
}
