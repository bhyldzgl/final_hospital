package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        Long departmentId, // opsiyonel: null ise doktorun departmanı kullanılır
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String note
) {}
