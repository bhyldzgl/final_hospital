package com.hospital.automation.domain.dto.response;

public record PatientSummaryResponse(
        Long id,
        String firstName,
        String lastName
) {}
