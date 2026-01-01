package com.hospital.automation.controller;

import com.hospital.automation.domain.dto.request.AdmissionCreateRequest;
import com.hospital.automation.domain.dto.request.AdmissionDischargeRequest;
import com.hospital.automation.domain.dto.response.AdmissionResponse;
import com.hospital.automation.service.AdmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admissions")
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
public class AdmissionController {

    private final AdmissionService admissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionResponse create(@Valid @RequestBody AdmissionCreateRequest request) {
        return admissionService.create(request);
    }

    @GetMapping
    public List<AdmissionResponse> getAll() {
        return admissionService.getAll();
    }

    @GetMapping("/{id}")
    public AdmissionResponse getById(@PathVariable Long id) {
        return admissionService.getById(id);
    }

    @PutMapping("/{id}/discharge")
    public AdmissionResponse discharge(@PathVariable Long id, @Valid @RequestBody AdmissionDischargeRequest request) {
        return admissionService.discharge(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        admissionService.delete(id);
    }
}
