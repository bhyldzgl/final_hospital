package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.VisitCreateRequest;
import com.hospital.automation.domain.dto.response.*;
import com.hospital.automation.domain.entity.*;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.repository.VisitRepository;
import com.hospital.automation.service.AuditLogService;
import com.hospital.automation.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    @Override
    public VisitResponse create(VisitCreateRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found: " + request.patientId()));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new NotFoundException("Doctor not found: " + request.doctorId()));

        Appointment appointment = null;
        if (request.appointmentId() != null) {
            appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new NotFoundException("Appointment not found: " + request.appointmentId()));
        }

        Visit visit = Visit.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .visitTime(request.visitTime())
                .complaint(request.complaint())
                .diagnosis(request.diagnosis())
                .build();

        Visit saved = visitRepository.save(visit);

        auditLogService.log(
                "CREATE",
                "Visit",
                saved.getId(),
                "Visit created (patientId=" + patient.getId() + ", doctorId=" + doctor.getId() +
                        (appointment != null ? ", appointmentId=" + appointment.getId() : "") + ")"
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitResponse> getAll() {
        return visitRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VisitResponse getById(Long id) {
        Visit v = visitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Visit not found: " + id));
        return toResponse(v);
    }

    @Override
    public void delete(Long id) {
        Visit v = visitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Visit not found: " + id));

        visitRepository.delete(v);

        auditLogService.log(
                "DELETE",
                "Visit",
                id,
                "Visit deleted (patientId=" + v.getPatient().getId() + ", doctorId=" + v.getDoctor().getId() + ")"
        );
    }

    private VisitResponse toResponse(Visit v) {
        Patient p = v.getPatient();
        Doctor d = v.getDoctor();

        PatientSummaryResponse ps = new PatientSummaryResponse(p.getId(), p.getFirstName(), p.getLastName());
        DoctorSummaryResponse ds = new DoctorSummaryResponse(d.getId(), d.getFirstName(), d.getLastName(), d.getSpecialization());

        Long appointmentId = (v.getAppointment() != null) ? v.getAppointment().getId() : null;

        List<MedicalRecordResponse> records = v.getMedicalRecords().stream()
                .map(r -> new MedicalRecordResponse(r.getId(), r.getRecordType(), r.getContent(), r.getCreatedAt()))
                .toList();

        List<PrescriptionResponse> prescriptions = v.getPrescriptions().stream()
                .map(pr -> new PrescriptionResponse(
                        pr.getId(),
                        pr.getCreatedAt(),
                        pr.getNote(),
                        pr.getItems().stream()
                                .map(i -> new PrescriptionItemResponse(i.getId(), i.getDrugName(), i.getDosage(), i.getFrequency(), i.getDurationDays(), i.getInstructions()))
                                .toList()
                ))
                .toList();

        return new VisitResponse(
                v.getId(),
                ps,
                ds,
                appointmentId,
                v.getVisitTime(),
                v.getComplaint(),
                v.getDiagnosis(),
                records,
                prescriptions
        );
    }
}
