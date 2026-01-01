package com.hospital.automation.domain.dto.response;

import com.hospital.automation.domain.enums.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        PatientSummaryResponse patient,
        DoctorSummaryResponse doctor,
        DepartmentResponse department,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String note
) {}
