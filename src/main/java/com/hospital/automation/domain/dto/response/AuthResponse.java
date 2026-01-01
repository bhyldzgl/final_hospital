package com.hospital.automation.domain.dto.response;

import java.util.Set;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        Set<String> roles
) {}
