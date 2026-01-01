package com.hospital.automation.repository;

import com.hospital.automation.domain.entity.Admission;
import com.hospital.automation.domain.enums.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    @Query("""
           SELECT COUNT(a)
           FROM Admission a
           WHERE a.room.id = :roomId
             AND a.status = :status
           """)
    long countByRoomIdAndStatus(@Param("roomId") Long roomId,
                                @Param("status") AdmissionStatus status);
}
