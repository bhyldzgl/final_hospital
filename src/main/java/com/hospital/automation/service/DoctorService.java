package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.DoctorCreateRequest;
import com.hospital.automation.domain.dto.response.DoctorResponse;

import java.util.List;

public interface DoctorService {
    DoctorResponse create(DoctorCreateRequest request);
    List<DoctorResponse> getAll();
    DoctorResponse getById(Long id);
    void delete(Long id);
}
