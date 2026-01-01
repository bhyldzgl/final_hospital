package com.hospital.automation.service.impl;

import com.hospital.automation.config.security.UserPrincipal;
import com.hospital.automation.domain.dto.response.AuditLogResponse;
import com.hospital.automation.domain.entity.AuditLog;
import com.hospital.automation.repository.AuditLogRepository;
import com.hospital.automation.repository.spec.AuditLogSpecifications;
import com.hospital.automation.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(String action, String entityName, Long entityId, String details) {
        String username = getCurrentUsername();

        AuditLog log = AuditLog.builder()
                .username(username)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .createdAt(LocalDateTime.now())
                .details(details)
                .build();

        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAll() {
        return auditLogRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String username, String action, String entityName,
                                         LocalDateTime from, LocalDateTime to,
                                         int page, int size, String sort) {

        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);

        Specification<AuditLog> spec = Specification
                .where(AuditLogSpecifications.usernameContains(username))
                .and(AuditLogSpecifications.actionEquals(action))
                .and(AuditLogSpecifications.entityNameEquals(entityName))
                .and(AuditLogSpecifications.createdAtGte(from))
                .and(AuditLogSpecifications.createdAtLte(to));

        return auditLogRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getUsername(),
                a.getAction(),
                a.getEntityName(),
                a.getEntityId(),
                a.getCreatedAt(),
                a.getDetails()
        );
    }

    private Sort parseSort(String sort) {
        // Default: createdAt desc
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        // örnek: "createdAt,desc" veya "username,asc"
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String dir = (parts.length > 1) ? parts[1].trim().toLowerCase() : "desc";

        Sort.Direction direction = dir.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return "SYSTEM";

        Object p = auth.getPrincipal();
        if (p instanceof UserPrincipal up) return up.getUsername();
        return auth.getName() != null ? auth.getName() : "SYSTEM";
    }
}
