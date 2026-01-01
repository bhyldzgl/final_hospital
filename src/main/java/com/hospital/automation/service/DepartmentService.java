package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.DepartmentCreateRequest;
import com.hospital.automation.domain.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse create(DepartmentCreateRequest request);
    List<DepartmentResponse> getAll();
    DepartmentResponse getById(Long id);
    void delete(Long id);
}
