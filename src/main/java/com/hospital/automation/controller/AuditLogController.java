package com.hospital.automation.controller;

import com.hospital.automation.domain.dto.response.AuditLogResponse;
import com.hospital.automation.domain.entity.AuditLog;
import com.hospital.automation.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    // Eski: hepsini getir (istersen tut)
    @GetMapping("/all")
    public List<AuditLog> getAll() {
        return auditLogService.getAll();
    }

    // Yeni: filtre + sayfalama + sıralama
    @GetMapping
    public Page<AuditLogResponse> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityName,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            // ör: createdAt,desc
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return auditLogService.search(username, action, entityName, from, to, page, size, sort);
    }
}
