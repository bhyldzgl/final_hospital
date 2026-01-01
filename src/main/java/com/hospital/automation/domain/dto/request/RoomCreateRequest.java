package com.hospital.automation.domain.dto.request;

import com.hospital.automation.domain.enums.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomCreateRequest(
        @NotBlank String roomNumber,
        Integer floor,
        @NotNull RoomType roomType,
        Integer capacity
) {}
