package com.hospital.automation.integration;

import com.hospital.automation.domain.dto.request.AdmissionCreateRequest;
import com.hospital.automation.domain.dto.request.AdmissionDischargeRequest;
import com.hospital.automation.domain.dto.request.PatientCreateRequest;
import com.hospital.automation.domain.dto.request.RoomCreateRequest;
import com.hospital.automation.domain.enums.RoomType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdmissionControllerIT extends IntegrationTestBase {

    @Test
    void receptionist_canCreateDischargeDeleteAdmission() throws Exception {

        // receptionist token
        var receptionist = createUserWithRoles(
                "receptAdm", "receptAdm@test.com", "pass12345",
                Set.of("ROLE_RECEPTIONIST")
        );
        String receptionistJwt = jwtFor(receptionist.getUsername(), Set.of("ROLE_RECEPTIONIST"));

        // admin token (room create için)
        var admin = createUserWithRoles(
                "adminAdm", "adminAdm@test.com", "pass12345",
                Set.of("ROLE_ADMIN")
        );
        String adminJwt = jwtFor(admin.getUsername(), Set.of("ROLE_ADMIN"));

        // 1) Patient create (receptionist yetkili)
        var patientReq = new PatientCreateRequest(
                "Ayse", "Yilmaz",
                LocalDate.of(1998, 5, 5),
                "11111111111",
                "5552223344",
                "Istanbul"
        );

        String patientRes = mockMvc.perform(post("/api/patients")
                        .header("Authorization", bearer(receptionistJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        long patientId = objectMapper.readTree(patientRes).get("id").asLong();

        // 2) Room create (admin yetkili)
        var roomReq = new RoomCreateRequest("B-201", 2, RoomType.PRIVATE, 1);

        String roomRes = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", bearer(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        long roomId = objectMapper.readTree(roomRes).get("id").asLong();

        // 3) Admission create (DTO birebir)
        var admittedAt = LocalDateTime.of(2026, 1, 3, 9, 0, 0);
        var admissionCreateReq = new AdmissionCreateRequest(
                patientId,
                roomId,
                null, // attendingDoctorId opsiyonel
                admittedAt,
                "Observation"
        );

        String admissionRes = mockMvc.perform(post("/api/admissions")
                        .header("Authorization", bearer(receptionistJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(admissionCreateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.patient.id").value(patientId))
                .andExpect(jsonPath("$.room.id").value(roomId))
                .andExpect(jsonPath("$.admittedAt").exists())
                .andReturn().getResponse().getContentAsString();

        long admissionId = objectMapper.readTree(admissionRes).get("id").asLong();

        // 4) Discharge (DTO birebir)
        var dischargedAt = LocalDateTime.of(2026, 1, 3, 12, 0, 0);
        var dischargeReq = new AdmissionDischargeRequest(
                dischargedAt,
                "Stable"
        );

        mockMvc.perform(put("/api/admissions/{id}/discharge", admissionId)
                        .header("Authorization", bearer(receptionistJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dischargeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admissionId))
                .andExpect(jsonPath("$.dischargedAt").exists())
                .andExpect(jsonPath("$.note").value("Stable"));

        // 5) Delete
        mockMvc.perform(delete("/api/admissions/{id}", admissionId)
                        .header("Authorization", bearer(receptionistJwt)))
                .andExpect(status().isNoContent());
    }

    @Test
    void doctorCannotAccessAdmissionsEndpoints() throws Exception {
        var doctorUser = createUserWithRoles(
                "docAdm", "docAdm@test.com", "pass12345",
                Set.of("ROLE_DOCTOR")
        );
        String jwt = jwtFor(doctorUser.getUsername(), Set.of("ROLE_DOCTOR"));

        mockMvc.perform(get("/api/admissions")
                        .header("Authorization", bearer(jwt)))
                .andExpect(status().isForbidden());
    }
}
