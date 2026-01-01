package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.AppointmentCreateRequest;
import com.hospital.automation.domain.dto.request.AppointmentUpdateRequest;
import com.hospital.automation.domain.dto.response.AppointmentResponse;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse create(AppointmentCreateRequest request);
    List<AppointmentResponse> getAll();
    AppointmentResponse getById(Long id);
    AppointmentResponse update(Long id, AppointmentUpdateRequest request);
    void delete(Long id);
}
