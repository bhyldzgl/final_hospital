package com.hospital.automation.controller;

import com.hospital.automation.domain.dto.request.PrescriptionCreateRequest;
import com.hospital.automation.domain.dto.response.PrescriptionResponse;
import com.hospital.automation.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prescriptions")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(@Valid @RequestBody PrescriptionCreateRequest request) {
        return prescriptionService.create(request);
    }
}
