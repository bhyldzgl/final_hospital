package com.hospital.automation.integration;

import com.hospital.automation.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditLogControllerIT extends IntegrationTestBase {

    @Autowired private AuditLogService auditLogService;

    @Test
    void onlyAdmin_canSearchAuditLogs() throws Exception {
        auditLogService.log("CREATE", "X", 1L, "details");

        var admin = createUserWithRoles("adL", "adL@test.com", "pass12345", Set.of("ROLE_ADMIN"));
        String adminJwt = jwtFor(admin.getUsername(), Set.of("ROLE_ADMIN"));

        var doctor = createUserWithRoles("doL", "doL@test.com", "pass12345", Set.of("ROLE_DOCTOR"));
        String doctorJwt = jwtFor(doctor.getUsername(), Set.of("ROLE_DOCTOR"));

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", bearer(doctorJwt)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", bearer(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
