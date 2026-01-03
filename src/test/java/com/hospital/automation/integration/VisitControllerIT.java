package com.hospital.automation.integration;

import com.hospital.automation.domain.dto.request.PatientCreateRequest;
import com.hospital.automation.domain.dto.request.VisitCreateRequest;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VisitControllerIT extends IntegrationTestBase {

    @Autowired private PatientService patientService;
    @Autowired private DoctorRepository doctorRepository;

    @Test
    void doctor_canCreateVisit_patientCannot() throws Exception {
        var doctorUser = createUserWithRoles("docU", "docU@test.com", "pass12345", Set.of("ROLE_DOCTOR"));
        String doctorJwt = jwtFor(doctorUser.getUsername(), Set.of("ROLE_DOCTOR"));

        var patientUser = createUserWithRoles("patU", "patU@test.com", "pass12345", Set.of("ROLE_PATIENT"));
        String patientJwt = jwtFor(patientUser.getUsername(), Set.of("ROLE_PATIENT"));

        var p = patientService.create(new PatientCreateRequest(
                "P", "One", LocalDate.of(1999,1,1), "12345678901", null, null
        ));

        Doctor d = doctorRepository.saveAndFlush(
                Doctor.builder().firstName("D").lastName("One").specialization("Cardiology").build()
        );

        VisitCreateRequest req = new VisitCreateRequest(
                p.id(),
                d.getId(),
                null,
                LocalDateTime.of(2026,1,3,14,0),
                "Headache",
                "Check"
        );

        // patient -> 403
        mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(patientJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // doctor -> 201
        mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(doctorJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.patient.id").value(p.id()))
                .andExpect(jsonPath("$.doctor.id").value(d.getId()))
                .andExpect(jsonPath("$.medicalRecords").isArray())
                .andExpect(jsonPath("$.prescriptions").isArray());
    }
}
