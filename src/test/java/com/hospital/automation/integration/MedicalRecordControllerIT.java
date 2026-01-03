package com.hospital.automation.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MedicalRecordControllerIT extends IntegrationTestBase {

    @Test
    void doctor_canCreateMedicalRecord_forExistingVisit() throws Exception {

        // 1) Create patient (admin OR receptionist allowed) - use admin
        String patientBody = """
                {
                  "firstName":"Mehmet",
                  "lastName":"Kaya",
                  "birthDate":"1995-02-10",
                  "nationalId":"%s",
                  "phone":"5551112233",
                  "address":"Kadikoy"
                }
                """.formatted(uniqueNationalId11());

        String patientJson = mockMvc.perform(post("/api/patients")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long patientId = objectMapper.readTree(patientJson).get("id").asLong();

        // 2) Create doctor (admin)
        String doctorBody = """
                {"firstName":"Dr","lastName":"House","specialization":"Internal"}
                """;

        String doctorJson = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long doctorId = objectMapper.readTree(doctorJson).get("id").asLong();

        // 3) Create visit (doctor allowed)
        String visitBody = """
                {
                  "patientId": %d,
                  "doctorId": %d,
                  "appointmentId": null,
                  "visitTime": "2026-01-03T13:00:00",
                  "complaint": "Headache",
                  "diagnosis": "Migraine"
                }
                """.formatted(patientId, doctorId);

        String visitJson = mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(visitBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long visitId = objectMapper.readTree(visitJson).get("id").asLong();

        // 4) Create medical record (doctor allowed)
        String recordBody = """
                {
                  "visitId": %d,
                  "recordType": "LAB",
                  "content": "Blood test results normal."
                }
                """.formatted(visitId);

        mockMvc.perform(post("/api/medical-records")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.recordType", is("LAB")))
                .andExpect(jsonPath("$.content", containsString("Blood test")));
    }
}
