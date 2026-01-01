package com.hospital.automation.repository.spec;

import com.hospital.automation.domain.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> usernameContains(String username) {
        return (root, query, cb) ->
                (username == null || username.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }

    public static Specification<AuditLog> actionEquals(String action) {
        return (root, query, cb) ->
                (action == null || action.isBlank())
                        ? cb.conjunction()
                        : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> entityNameEquals(String entityName) {
        return (root, query, cb) ->
                (entityName == null || entityName.isBlank())
                        ? cb.conjunction()
                        : cb.equal(root.get("entityName"), entityName);
    }

    public static Specification<AuditLog> createdAtGte(LocalDateTime from) {
        return (root, query, cb) ->
                (from == null)
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdAtLte(LocalDateTime to) {
        return (root, query, cb) ->
                (to == null)
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
