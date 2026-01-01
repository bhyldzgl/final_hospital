package com.hospital.automation.domain.dto.response;

public record DoctorResponse(
        Long id,
        String firstName,
        String lastName,
        String specialization,
        DepartmentResponse department
) {}
