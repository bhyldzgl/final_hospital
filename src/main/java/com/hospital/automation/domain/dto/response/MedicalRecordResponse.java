package com.hospital.automation.domain.dto.response;

import java.time.LocalDateTime;

public record MedicalRecordResponse(
        Long id,
        String recordType,
        String content,
        LocalDateTime createdAt
) {}
