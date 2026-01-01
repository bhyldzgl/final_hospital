package com.hospital.automation.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patients")
@ToString(exclude = {"appointments", "visits", "admissions"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    private LocalDate birthDate;

    @Column(unique = true, length = 20)
    private String nationalId; // TC kimlik vs.

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String address;

    // Hasta user’a bağlanabilir (portal vs.)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<Visit> visits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
    private List<Admission> admissions = new ArrayList<>();
}
