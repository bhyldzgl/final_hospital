package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.LoginRequest;
import com.hospital.automation.domain.dto.request.RegisterRequest;
import com.hospital.automation.domain.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
