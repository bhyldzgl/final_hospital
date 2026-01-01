package com.hospital.automation.service;

import com.hospital.automation.domain.dto.response.AuditLogResponse;
import com.hospital.automation.domain.entity.AuditLog;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {
    void log(String action, String entityName, Long entityId, String details);

    // eski
    List<AuditLog> getAll();

    // yeni
    Page<AuditLogResponse> search(
            String username,
            String action,
            String entityName,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            String sort
    );
}
