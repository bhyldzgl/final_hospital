package com.hospital.automation.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "prescription_items")
@ToString(exclude = "prescription")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(nullable = false, length = 120)
    private String drugName;

    @Column(length = 80)
    private String dosage; // 500mg vb.

    @Column(length = 80)
    private String frequency; // günde 2 vb.

    private Integer durationDays; // kaç gün

    @Column(length = 200)
    private String instructions; // aç/tok vb.
}
