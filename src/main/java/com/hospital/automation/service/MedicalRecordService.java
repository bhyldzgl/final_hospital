package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.MedicalRecordCreateRequest;
import com.hospital.automation.domain.dto.response.MedicalRecordResponse;

public interface MedicalRecordService {
    MedicalRecordResponse create(MedicalRecordCreateRequest request);
}
