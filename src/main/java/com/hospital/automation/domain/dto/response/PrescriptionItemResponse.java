package com.hospital.automation.domain.dto.response;

public record PrescriptionItemResponse(
        Long id,
        String drugName,
        String dosage,
        String frequency,
        Integer durationDays,
        String instructions
) {}
