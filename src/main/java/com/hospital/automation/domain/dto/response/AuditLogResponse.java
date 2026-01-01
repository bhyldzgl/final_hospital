package com.hospital.automation.domain.dto.response;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String username,
        String action,
        String entityName,
        Long entityId,
        LocalDateTime createdAt,
        String details
) {}
