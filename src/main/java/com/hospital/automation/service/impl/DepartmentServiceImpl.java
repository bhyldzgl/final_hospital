package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.DepartmentCreateRequest;
import com.hospital.automation.domain.dto.response.DepartmentResponse;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public DepartmentResponse create(DepartmentCreateRequest request) {
        departmentRepository.findByName(request.name())
                .ifPresent(d -> { throw new BadRequestException("Department already exists"); });

        Department saved = departmentRepository.save(
                Department.builder().name(request.name()).build()
        );
        return new DepartmentResponse(saved.getId(), saved.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll()
                .stream()
                .map(d -> new DepartmentResponse(d.getId(), d.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found: " + id));
        return new DepartmentResponse(d.getId(), d.getName());
    }

    @Override
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new NotFoundException("Department not found: " + id);
        }
        departmentRepository.deleteById(id);
    }
}
