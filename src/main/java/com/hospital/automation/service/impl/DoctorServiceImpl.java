package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.DoctorCreateRequest;
import com.hospital.automation.domain.dto.response.DepartmentResponse;
import com.hospital.automation.domain.dto.response.DoctorResponse;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public DoctorResponse create(DoctorCreateRequest request) {
        Department dept = null;
        if (request.departmentId() != null) {
            dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new NotFoundException("Department not found: " + request.departmentId()));
        }

        Doctor doctor = Doctor.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .specialization(request.specialization())
                .department(dept)
                .build();

        Doctor saved = doctorRepository.save(doctor);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAll() {
        return doctorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor not found: " + id));
        return toResponse(doctor);
    }

    @Override
    public void delete(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new NotFoundException("Doctor not found: " + id);
        }
        doctorRepository.deleteById(id);
    }

    private DoctorResponse toResponse(Doctor d) {
        DepartmentResponse dept = null;
        if (d.getDepartment() != null) {
            dept = new DepartmentResponse(d.getDepartment().getId(), d.getDepartment().getName());
        }
        return new DoctorResponse(
                d.getId(),
                d.getFirstName(),
                d.getLastName(),
                d.getSpecialization(),
                dept
        );
    }
}
