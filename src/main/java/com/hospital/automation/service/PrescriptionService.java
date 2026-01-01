package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.PrescriptionCreateRequest;
import com.hospital.automation.domain.dto.response.PrescriptionResponse;

public interface PrescriptionService {
    PrescriptionResponse create(PrescriptionCreateRequest request);
}
