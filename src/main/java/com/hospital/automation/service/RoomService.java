package com.hospital.automation.service;

import com.hospital.automation.domain.dto.request.RoomCreateRequest;
import com.hospital.automation.domain.dto.response.RoomResponse;

import java.util.List;

public interface RoomService {
    RoomResponse create(RoomCreateRequest request);
    List<RoomResponse> getAll();
    RoomResponse getById(Long id);
    void delete(Long id);
}
