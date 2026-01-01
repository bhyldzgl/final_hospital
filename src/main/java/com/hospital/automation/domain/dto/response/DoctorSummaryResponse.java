package com.hospital.automation.domain.dto.response;

public record DoctorSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String specialization
) {}
