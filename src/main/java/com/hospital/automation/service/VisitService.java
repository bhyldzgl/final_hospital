package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.VisitCreateRequest;
import com.hospital.automation.domain.dto.response.VisitResponse;

import java.util.List;

public interface VisitService {
    VisitResponse create(VisitCreateRequest request);
    List<VisitResponse> getAll();
    VisitResponse getById(Long id);
    void delete(Long id);
}
