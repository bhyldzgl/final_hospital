package com.hospital.automation.domain.dto.request;

import com.hospital.automation.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentUpdateRequest(
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull AppointmentStatus status,
        String note
) {}
