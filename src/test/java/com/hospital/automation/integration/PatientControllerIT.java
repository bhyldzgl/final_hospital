package com.hospital.automation.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PatientControllerIT extends IntegrationTestBase {

    @Test
    void receptionist_canCRUD_patient_doctorCannot() throws Exception {

        // Doctor cannot list patients
        mockMvc.perform(get("/api/patients")
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isForbidden());

        // Receptionist creates patient (UNIQUE nationalId)
        String nationalId = uniqueNationalId11();

        String createBody = """
                {
                  "firstName":"Ali",
                  "lastName":"Veli",
                  "birthDate":"2000-01-01",
                  "nationalId":"%s",
                  "phone":"5551112233",
                  "address":"Istanbul"
                }
                """.formatted(nationalId);

        String createdJson = mockMvc.perform(post("/api/patients")
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nationalId", is(nationalId)))
                .andReturn().getResponse().getContentAsString();

        Long patientId = objectMapper.readTree(createdJson).get("id").asLong();

        // Receptionist can get by id
        mockMvc.perform(get("/api/patients/{id}", patientId)
                        .header("Authorization", bearer(receptionistToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(patientId.intValue())));

        // Update
        String updateBody = """
                {
                  "firstName":"Ali2",
                  "lastName":"Veli2",
                  "birthDate":"2000-01-02",
                  "nationalId":"%s",
                  "phone":"5550000000",
                  "address":"Kadikoy"
                }
                """.formatted(nationalId);

        mockMvc.perform(put("/api/patients/{id}", patientId)
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Ali2")))
                .andExpect(jsonPath("$.address", is("Kadikoy")));

        // Delete
        mockMvc.perform(delete("/api/patients/{id}", patientId)
                        .header("Authorization", bearer(receptionistToken)))
                .andExpect(status().isNoContent());
    }
}
