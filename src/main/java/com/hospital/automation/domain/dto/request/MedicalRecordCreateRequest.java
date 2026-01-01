package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MedicalRecordCreateRequest(
        @NotNull Long visitId,
        @NotBlank @Size(max = 80) String recordType,
        @NotBlank @Size(max = 2000) String content
) {}
