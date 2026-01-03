package com.hospital.automation.integration;

import com.hospital.automation.domain.dto.request.RoomCreateRequest;
import com.hospital.automation.domain.enums.RoomType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoomControllerIT extends IntegrationTestBase {

    @Test
    void admin_canCreateRoom_nonAdminCannot() throws Exception {
        var admin = createUserWithRoles("ra", "ra@test.com", "pass12345", Set.of("ROLE_ADMIN"));
        String adminJwt = jwtFor(admin.getUsername(), Set.of("ROLE_ADMIN"));

        var patient = createUserWithRoles("rp", "rp@test.com", "pass12345", Set.of("ROLE_PATIENT"));
        String patientJwt = jwtFor(patient.getUsername(), Set.of("ROLE_PATIENT"));

        RoomCreateRequest req = new RoomCreateRequest("A-101", 1, RoomType.PRIVATE, 1);

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", bearer(patientJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", bearer(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("A-101"));
    }
}
