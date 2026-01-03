package com.hospital.automation.integration;

import com.hospital.automation.domain.dto.request.PatientCreateRequest;
import com.hospital.automation.domain.dto.request.PrescriptionCreateRequest;
import com.hospital.automation.domain.dto.request.PrescriptionItemCreateRequest;
import com.hospital.automation.domain.dto.request.VisitCreateRequest;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PrescriptionControllerIT extends IntegrationTestBase {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    void doctor_canCreatePrescription_withItems() throws Exception {
        // --- users & tokens ---
        var adminUser = createUserWithRoles("admin_pr", "admin_pr@test.com", "pass12345", Set.of("ROLE_ADMIN"));
        String adminJwt = jwtFor(adminUser.getUsername(), Set.of("ROLE_ADMIN"));

        var doctorUser = createUserWithRoles("doctor_pr", "doctor_pr@test.com", "pass12345", Set.of("ROLE_DOCTOR"));
        String doctorJwt = jwtFor(doctorUser.getUsername(), Set.of("ROLE_DOCTOR"));

        // --- patient ---
        var patientReq = new PatientCreateRequest(
                "Zeynep",
                "Arslan",
                LocalDate.of(2000, 7, 20),
                "88888888888",
                "5553332211",
                "Uskudar"
        );

        String patientRes = mockMvc.perform(post("/api/patients")
                        .header("Authorization", bearer(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long patientId = objectMapper.readTree(patientRes).get("id").asLong();

        // --- department + doctor (fixture) ---
        Department dep = departmentRepository.save(Department.builder().name("Internal Medicine").build());

        // ⚠️ Doctor entity alanları projene göre farklıysa burada düzelt:
        Doctor doctor = doctorRepository.save(
                Doctor.builder()
                        .firstName("Elif")
                        .lastName("Yildiz")
                        .specialization("Internal Medicine")
                        .department(dep)
                        .build()
        );

        long doctorId = doctor.getId();

        // --- visit ---
        var visitReq = new VisitCreateRequest(
                patientId,
                doctorId,
                null,
                LocalDateTime.of(2026, 1, 3, 11, 0, 0),
                "Fever",
                "Upper respiratory infection"
        );

        String visitRes = mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(doctorJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long visitId = objectMapper.readTree(visitRes).get("id").asLong();

        // --- prescription ---
        var items = List.of(
                new PrescriptionItemCreateRequest("Paracetamol", "500mg", "2x/day", 3, "After meal"),
                new PrescriptionItemCreateRequest("Vitamin C", "1000mg", "1x/day", 5, "Morning")
        );

        var prReq = new PrescriptionCreateRequest(
                visitId,
                "Take as prescribed",
                items
        );

        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(doctorJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.note").value("Take as prescribed"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].drugName").value("Paracetamol"))
                .andExpect(jsonPath("$.items[1].drugName").value("Vitamin C"));
    }

    @Test
    void receptionist_cannotCreatePrescription_forbidden() throws Exception {
        var receptionistUser = createUserWithRoles(
                "recept_pr", "recept_pr@test.com", "pass12345",
                Set.of("ROLE_RECEPTIONIST")
        );
        String jwt = jwtFor(receptionistUser.getUsername(), Set.of("ROLE_RECEPTIONIST"));

        var prReq = new PrescriptionCreateRequest(1L, "x", null);

        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", bearer(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prReq)))
                .andExpect(status().isForbidden());
    }
}
