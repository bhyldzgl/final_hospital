package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PrescriptionCreateRequest(
        @NotNull Long visitId,
        String note,
        List<PrescriptionItemCreateRequest> items
) {}
