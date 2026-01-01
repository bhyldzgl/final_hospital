package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.PatientCreateRequest;
import com.hospital.automation.domain.dto.request.PatientUpdateRequest;
import com.hospital.automation.domain.dto.response.PatientResponse;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.service.AuditLogService;
import com.hospital.automation.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final AuditLogService auditLogService;

    @Override
    public PatientResponse create(PatientCreateRequest request) {
        if (request.nationalId() != null && !request.nationalId().isBlank()) {
            patientRepository.findByNationalId(request.nationalId())
                    .ifPresent(p -> { throw new BadRequestException("nationalId already exists"); });
        }

        Patient patient = Patient.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .birthDate(request.birthDate())
                .nationalId(request.nationalId())
                .phone(request.phone())
                .address(request.address())
                .build();

        Patient saved = patientRepository.save(patient);

        auditLogService.log(
                "CREATE",
                "Patient",
                saved.getId(),
                "Patient created: " + saved.getFirstName() + " " + saved.getLastName()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAll() {
        return patientRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));
        return toResponse(patient);
    }

    @Override
    public PatientResponse update(Long id, PatientUpdateRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));

        if (request.nationalId() != null && !request.nationalId().isBlank()) {
            patientRepository.findByNationalId(request.nationalId())
                    .filter(p -> !p.getId().equals(id))
                    .ifPresent(p -> { throw new BadRequestException("nationalId already exists"); });
        }

        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setBirthDate(request.birthDate());
        patient.setNationalId(request.nationalId());
        patient.setPhone(request.phone());
        patient.setAddress(request.address());

        auditLogService.log(
                "UPDATE",
                "Patient",
                patient.getId(),
                "Patient updated: " + patient.getFirstName() + " " + patient.getLastName()
        );

        return toResponse(patient);
    }

    @Override
    public void delete(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));

        patientRepository.delete(patient);

        auditLogService.log(
                "DELETE",
                "Patient",
                id,
                "Patient deleted: " + patient.getFirstName() + " " + patient.getLastName()
        );
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getBirthDate(),
                p.getNationalId(),
                p.getPhone(),
                p.getAddress()
        );
    }
}
