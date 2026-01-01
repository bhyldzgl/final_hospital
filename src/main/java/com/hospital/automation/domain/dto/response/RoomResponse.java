package com.hospital.automation.domain.dto.response;

import com.hospital.automation.domain.enums.RoomType;

public record RoomResponse(
        Long id,
        String roomNumber,
        Integer floor,
        RoomType roomType,
        Integer capacity
) {}
