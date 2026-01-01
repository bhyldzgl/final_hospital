package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionItemCreateRequest(
        @NotBlank @Size(max = 120) String drugName,
        @Size(max = 80) String dosage,
        @Size(max = 80) String frequency,
        Integer durationDays,
        @Size(max = 200) String instructions
) {}
