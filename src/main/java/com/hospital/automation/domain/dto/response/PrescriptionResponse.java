package com.hospital.automation.domain.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponse(
        Long id,
        LocalDateTime createdAt,
        String note,
        List<PrescriptionItemResponse> items
) {}
