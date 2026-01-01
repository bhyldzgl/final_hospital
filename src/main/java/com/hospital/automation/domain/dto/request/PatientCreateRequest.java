package com.hospital.automation.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientCreateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        LocalDate birthDate,
        @Size(max = 20) String nationalId,
        @Size(max = 30) String phone,
        @Size(max = 200) String address
) {}
