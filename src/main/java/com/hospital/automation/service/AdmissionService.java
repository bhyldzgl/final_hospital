package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.AdmissionCreateRequest;
import com.hospital.automation.domain.dto.request.AdmissionDischargeRequest;
import com.hospital.automation.domain.dto.response.AdmissionResponse;

import java.util.List;

public interface AdmissionService {
    AdmissionResponse create(AdmissionCreateRequest request);
    List<AdmissionResponse> getAll();
    AdmissionResponse getById(Long id);
    AdmissionResponse discharge(Long id, AdmissionDischargeRequest request);
    void delete(Long id);
}
