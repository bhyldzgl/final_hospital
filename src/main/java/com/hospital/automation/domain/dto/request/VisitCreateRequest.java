package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record VisitCreateRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        Long appointmentId,          // opsiyonel
        @NotNull LocalDateTime visitTime,
        String complaint,
        String diagnosis
) {}
