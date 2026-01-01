package com.hospital.automation.domain.dto.response;

import com.hospital.automation.domain.enums.AdmissionStatus;

import java.time.LocalDateTime;

public record AdmissionResponse(
        Long id,
        PatientSummaryResponse patient,
        RoomResponse room,
        DoctorSummaryResponse attendingDoctor,
        LocalDateTime admittedAt,
        LocalDateTime dischargedAt,
        AdmissionStatus status,
        String note
) {}
