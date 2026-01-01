package com.hospital.automation.controller;

import com.hospital.automation.domain.dto.request.MedicalRecordCreateRequest;
import com.hospital.automation.domain.dto.response.MedicalRecordResponse;
import com.hospital.automation.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/medical-records")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalRecordResponse create(@Valid @RequestBody MedicalRecordCreateRequest request) {
        return medicalRecordService.create(request);
    }
}
