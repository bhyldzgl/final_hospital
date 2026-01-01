package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AdmissionDischargeRequest(
        @NotNull LocalDateTime dischargedAt,
        String note
) {}
