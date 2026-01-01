package com.hospital.automation.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false, length = 80)
    private String entityName;

    private Long entityId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 2000)
    private String details;
}
