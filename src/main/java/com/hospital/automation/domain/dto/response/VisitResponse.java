package com.hospital.automation.domain.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record VisitResponse(
        Long id,
        PatientSummaryResponse patient,
        DoctorSummaryResponse doctor,
        Long appointmentId,
        LocalDateTime visitTime,
        String complaint,
        String diagnosis,
        List<MedicalRecordResponse> medicalRecords,
        List<PrescriptionResponse> prescriptions
) {}
