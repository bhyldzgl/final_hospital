package com.hospital.automation.domain.entity;

import com.hospital.automation.domain.enums.AdmissionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admissions")
@ToString(exclude = {"patient", "room", "attendingDoctor"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attending_doctor_id")
    private Doctor attendingDoctor;

    @Column(nullable = false)
    private LocalDateTime admittedAt;

    private LocalDateTime dischargedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionStatus status;

    @Column(length = 500)
    private String note;
}
