package com.hospital.automation.repository;

import com.hospital.automation.domain.entity.Appointment;
import com.hospital.automation.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Çakışma: mevcut.start < yeniEnd AND mevcut.end > yeniStart
    @Query("""
           SELECT COUNT(a) > 0
           FROM Appointment a
           WHERE a.doctor.id = :doctorId
             AND a.status = :status
             AND a.startTime < :endTime
             AND a.endTime > :startTime
           """)
    boolean existsOverlappingAppointment(
            @Param("doctorId") Long doctorId,
            @Param("status") AppointmentStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
           SELECT COUNT(a) > 0
           FROM Appointment a
           WHERE a.doctor.id = :doctorId
             AND a.id <> :excludeId
             AND a.status = :status
             AND a.startTime < :endTime
             AND a.endTime > :startTime
           """)
    boolean existsOverlappingAppointmentExcludingId(
            @Param("doctorId") Long doctorId,
            @Param("excludeId") Long excludeId,
            @Param("status") AppointmentStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
