package com.hospital.automation.domain.dto.response;

import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String nationalId,
        String phone,
        String address
) {}
