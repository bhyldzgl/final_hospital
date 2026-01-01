package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AdmissionCreateRequest(
        @NotNull Long patientId,
        @NotNull Long roomId,
        Long attendingDoctorId,     // opsiyonel
        @NotNull LocalDateTime admittedAt,
        String note
) {}
