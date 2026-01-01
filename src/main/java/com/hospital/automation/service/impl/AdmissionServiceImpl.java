package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.AdmissionCreateRequest;
import com.hospital.automation.domain.dto.request.AdmissionDischargeRequest;
import com.hospital.automation.domain.dto.response.AdmissionResponse;
import com.hospital.automation.domain.dto.response.DoctorSummaryResponse;
import com.hospital.automation.domain.dto.response.PatientSummaryResponse;
import com.hospital.automation.domain.dto.response.RoomResponse;
import com.hospital.automation.domain.entity.Admission;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.domain.entity.Room;
import com.hospital.automation.domain.enums.AdmissionStatus;
import com.hospital.automation.repository.AdmissionRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.repository.RoomRepository;
import com.hospital.automation.service.AdmissionService;
import com.hospital.automation.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdmissionServiceImpl implements AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;
    private final RoomRepository roomRepository;
    private final DoctorRepository doctorRepository;
    private final AuditLogService auditLogService;

    @Override
    public AdmissionResponse create(AdmissionCreateRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found: " + request.patientId()));

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new NotFoundException("Room not found: " + request.roomId()));

        Doctor doctor = null;
        if (request.attendingDoctorId() != null) {
            doctor = doctorRepository.findById(request.attendingDoctorId())
                    .orElseThrow(() -> new NotFoundException("Doctor not found: " + request.attendingDoctorId()));
        }

        if (room.getCapacity() != null) {
            long activeCount = admissionRepository.countByRoomIdAndStatus(room.getId(), AdmissionStatus.ADMITTED);
            if (activeCount >= room.getCapacity()) {
                throw new BadRequestException("Room is full (capacity reached)");
            }
        }

        Admission admission = Admission.builder()
                .patient(patient)
                .room(room)
                .attendingDoctor(doctor)
                .admittedAt(request.admittedAt())
                .status(AdmissionStatus.ADMITTED)
                .note(request.note())
                .build();

        Admission saved = admissionRepository.save(admission);

        auditLogService.log(
                "CREATE",
                "Admission",
                saved.getId(),
                "Admission created (patientId=" + patient.getId() + ", roomId=" + room.getId() + ")"
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdmissionResponse> getAll() {
        return admissionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionResponse getById(Long id) {
        Admission a = admissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + id));
        return toResponse(a);
    }

    @Override
    public AdmissionResponse discharge(Long id, AdmissionDischargeRequest request) {
        Admission a = admissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + id));

        if (a.getStatus() == AdmissionStatus.DISCHARGED) {
            throw new BadRequestException("Admission is already discharged");
        }

        if (request.dischargedAt().isBefore(a.getAdmittedAt())) {
            throw new BadRequestException("dischargedAt cannot be before admittedAt");
        }

        a.setDischargedAt(request.dischargedAt());
        a.setStatus(AdmissionStatus.DISCHARGED);
        if (request.note() != null) {
            a.setNote(request.note());
        }

        auditLogService.log(
                "DISCHARGE",
                "Admission",
                a.getId(),
                "Admission discharged (patientId=" + a.getPatient().getId() + ", roomId=" + a.getRoom().getId() + ")"
        );

        return toResponse(a);
    }

    @Override
    public void delete(Long id) {
        Admission a = admissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admission not found: " + id));

        admissionRepository.delete(a);

        auditLogService.log(
                "DELETE",
                "Admission",
                id,
                "Admission deleted (patientId=" + a.getPatient().getId() + ", roomId=" + a.getRoom().getId() + ")"
        );
    }

    private AdmissionResponse toResponse(Admission a) {
        Patient p = a.getPatient();
        Room r = a.getRoom();

        PatientSummaryResponse patient = new PatientSummaryResponse(p.getId(), p.getFirstName(), p.getLastName());
        RoomResponse room = new RoomResponse(r.getId(), r.getRoomNumber(), r.getFloor(), r.getRoomType(), r.getCapacity());

        DoctorSummaryResponse doc = null;
        if (a.getAttendingDoctor() != null) {
            Doctor d = a.getAttendingDoctor();
            doc = new DoctorSummaryResponse(d.getId(), d.getFirstName(), d.getLastName(), d.getSpecialization());
        }

        return new AdmissionResponse(
                a.getId(),
                patient,
                room,
                doc,
                a.getAdmittedAt(),
                a.getDischargedAt(),
                a.getStatus(),
                a.getNote()
        );
    }
}
