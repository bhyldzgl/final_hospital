package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.PatientCreateRequest;
import com.hospital.automation.domain.dto.request.PatientUpdateRequest;
import com.hospital.automation.domain.dto.response.PatientResponse;

import java.util.List;

public interface PatientService {
    PatientResponse create(PatientCreateRequest request);
    List<PatientResponse> getAll();
    PatientResponse getById(Long id);
    PatientResponse update(Long id, PatientUpdateRequest request);
    void delete(Long id);
}
