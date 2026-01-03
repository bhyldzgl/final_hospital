package com.hospital.automation.integration;

import com.hospital.automation.domain.dto.request.LoginRequest;
import com.hospital.automation.domain.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends IntegrationTestBase {

    @Test
    void register_shouldReturnToken_andRoles() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "u1",
                "u1@test.com",
                "password123",
                Set.of("PATIENT") // prefixsiz -> normalize: ROLE_PATIENT
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("u1"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void login_shouldReturnToken_whenCredentialsValid() throws Exception {
        // önce register
        RegisterRequest reg = new RegisterRequest("u2", "u2@test.com", "password123", Set.of("PATIENT"));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        // login
        LoginRequest login = new LoginRequest("u2", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("u2"));
    }

    @Test
    void me_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden()); // security chain -> authenticated lazım
    }

    @Test
    void me_shouldReturnUserInfo_whenTokenProvided() throws Exception {
        var u = createUserWithRoles(
                "meuser", "meuser@test.com", "pass12345",
                Set.of("ROLE_ADMIN")
        );
        String jwt = jwtFor(u.getUsername(), Set.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("meuser"))
                .andExpect(jsonPath("$.roles").isArray());
    }
}
