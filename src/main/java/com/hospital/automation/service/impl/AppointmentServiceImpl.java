package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.AppointmentCreateRequest;
import com.hospital.automation.domain.dto.request.AppointmentUpdateRequest;
import com.hospital.automation.domain.dto.response.*;
import com.hospital.automation.domain.entity.Appointment;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.domain.enums.AppointmentStatus;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.service.AppointmentService;
import com.hospital.automation.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    @Override
    public AppointmentResponse create(AppointmentCreateRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found: " + request.patientId()));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new NotFoundException("Doctor not found: " + request.doctorId()));

        Department dept = resolveDepartment(request.departmentId(), doctor);

        boolean overlap = appointmentRepository.existsOverlappingAppointment(
                doctor.getId(),
                AppointmentStatus.SCHEDULED,
                request.startTime(),
                request.endTime()
        );

        if (overlap) {
            throw new BadRequestException("Doctor has another appointment in this time range");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .department(dept)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(AppointmentStatus.SCHEDULED)
                .note(request.note())
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        auditLogService.log(
                "CREATE",
                "Appointment",
                saved.getId(),
                "Appointment created (patientId=" + patient.getId() + ", doctorId=" + doctor.getId() + ")"
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAll() {
        return appointmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
        return toResponse(a);
    }

    @Override
    public AppointmentResponse update(Long id, AppointmentUpdateRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));

        if (request.status() == AppointmentStatus.SCHEDULED) {
            boolean overlap = appointmentRepository.existsOverlappingAppointmentExcludingId(
                    a.getDoctor().getId(),
                    a.getId(),
                    AppointmentStatus.SCHEDULED,
                    request.startTime(),
                    request.endTime()
            );
            if (overlap) {
                throw new BadRequestException("Doctor has another appointment in this time range");
            }
        }

        a.setStartTime(request.startTime());
        a.setEndTime(request.endTime());
        a.setStatus(request.status());
        a.setNote(request.note());

        auditLogService.log(
                "UPDATE",
                "Appointment",
                a.getId(),
                "Appointment updated (status=" + a.getStatus() + ")"
        );

        return toResponse(a);
    }

    @Override
    public void delete(Long id) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));

        appointmentRepository.delete(a);

        auditLogService.log(
                "DELETE",
                "Appointment",
                id,
                "Appointment deleted (doctorId=" + a.getDoctor().getId() + ", patientId=" + a.getPatient().getId() + ")"
        );
    }

    private void validateTimeRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private Department resolveDepartment(Long departmentId, Doctor doctor) {
        if (departmentId != null) {
            return departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new NotFoundException("Department not found: " + departmentId));
        }
        return doctor.getDepartment();
    }

    private AppointmentResponse toResponse(Appointment a) {
        Patient p = a.getPatient();
        Doctor d = a.getDoctor();

        PatientSummaryResponse patient = new PatientSummaryResponse(p.getId(), p.getFirstName(), p.getLastName());
        DoctorSummaryResponse doctor = new DoctorSummaryResponse(d.getId(), d.getFirstName(), d.getLastName(), d.getSpecialization());

        DepartmentResponse dept = null;
        if (a.getDepartment() != null) {
            dept = new DepartmentResponse(a.getDepartment().getId(), a.getDepartment().getName());
        }

        return new AppointmentResponse(
                a.getId(),
                patient,
                doctor,
                dept,
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getNote()
        );
    }
}
