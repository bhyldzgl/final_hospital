package com.hospital.automation.integration;

import com.hospital.automation.domain.dto.request.DepartmentCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DepartmentControllerIT extends IntegrationTestBase {

    @Test
    void endpoints_shouldBeForbidden_withoutToken() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturn403_forNonAdmin() throws Exception {
        var u = createUserWithRoles("p1", "p1@test.com", "pass12345", Set.of("ROLE_PATIENT"));
        String jwt = jwtFor(u.getUsername(), Set.of("ROLE_PATIENT"));

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", bearer(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentCreateRequest("Cardiology"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldWork_forAdmin_andRejectDuplicate() throws Exception {
        var admin = createUserWithRoles("a1", "a1@test.com", "pass12345", Set.of("ROLE_ADMIN"));
        String jwt = jwtFor(admin.getUsername(), Set.of("ROLE_ADMIN"));

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", bearer(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentCreateRequest("Cardiology"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Cardiology"));

        // duplicate -> 400 (BadRequestException)
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", bearer(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentCreateRequest("Cardiology"))))
                .andExpect(status().isBadRequest());
    }
}
