package com.hospital.automation.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AppointmentControllerIT extends IntegrationTestBase {

    @Test
    void receptionist_canCreateAppointment_andOverlappingShouldFail() throws Exception {

        // 1) Doctor create (admin)
        String doctorBody = """
                {"firstName":"Doc","lastName":"One","specialization":"Cardiology"}
                """;

        String doctorJson = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long doctorId = objectMapper.readTree(doctorJson).get("id").asLong();

        // 2) Patient create (receptionist) - UNIQUE nationalId
        String patientBody = """
                {
                  "firstName":"Ali",
                  "lastName":"Veli",
                  "birthDate":"2000-01-01",
                  "nationalId":"%s",
                  "phone":"5550000000",
                  "address":"Istanbul"
                }
                """.formatted(uniqueNationalId11());

        String patientJson = mockMvc.perform(post("/api/patients")
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long patientId = objectMapper.readTree(patientJson).get("id").asLong();

        // 3) Create appointment
        String createBody = """
                {
                  "patientId": %d,
                  "doctorId": %d,
                  "startTime": "2026-01-03T10:00:00",
                  "endTime": "2026-01-03T10:30:00",
                  "notes": "Initial appointment"
                }
                """.formatted(patientId, doctorId);

        String createdApptJson = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("SCHEDULED")))
                .andReturn().getResponse().getContentAsString();

        Long apptId = objectMapper.readTree(createdApptJson).get("id").asLong();

        // 4) Overlapping should fail
        String overlappingBody = """
                {
                  "patientId": %d,
                  "doctorId": %d,
                  "startTime": "2026-01-03T10:15:00",
                  "endTime": "2026-01-03T10:45:00",
                  "notes": "Overlapping"
                }
                """.formatted(patientId, doctorId);

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingBody))
                .andExpect(status().isBadRequest());

        // 5) Update appointment (FIX: status gönderiyoruz!)
        String updateBody = """
                {
                  "patientId": %d,
                  "doctorId": %d,
                  "startTime": "2026-01-03T11:00:00",
                  "endTime": "2026-01-03T11:30:00",
                  "status": "SCHEDULED",
                  "notes": "Updated"
                }
                """.formatted(patientId, doctorId);

        mockMvc.perform(put("/api/appointments/{id}", apptId)
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(apptId.intValue())))
                .andExpect(jsonPath("$.startTime", is("2026-01-03T11:00:00")))
                .andExpect(jsonPath("$.endTime", is("2026-01-03T11:30:00")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));
    }

    @Test
    void receptionist_canDeleteAppointment() throws Exception {
        // Basit smoke: doktor + hasta + randevu create, sonra delete

        String doctorBody = """
                {"firstName":"Doc","lastName":"Del","specialization":"Dermatology"}
                """;

        String doctorJson = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long doctorId = objectMapper.readTree(doctorJson).get("id").asLong();

        String patientBody = """
                {
                  "firstName":"Ayse",
                  "lastName":"Yilmaz",
                  "birthDate":"1999-01-01",
                  "nationalId":"%s",
                  "phone":"5551231231",
                  "address":"Istanbul"
                }
                """.formatted(uniqueNationalId11());

        String patientJson = mockMvc.perform(post("/api/patients")
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long patientId = objectMapper.readTree(patientJson).get("id").asLong();

        String createBody = """
                {
                  "patientId": %d,
                  "doctorId": %d,
                  "startTime": "2026-01-03T12:00:00",
                  "endTime": "2026-01-03T12:30:00",
                  "notes": "to delete"
                }
                """.formatted(patientId, doctorId);

        String apptJson = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long apptId = objectMapper.readTree(apptJson).get("id").asLong();

        mockMvc.perform(delete("/api/appointments/{id}", apptId)
                        .header("Authorization", bearer(receptionistToken)))
                .andExpect(status().isNoContent());
    }
}
