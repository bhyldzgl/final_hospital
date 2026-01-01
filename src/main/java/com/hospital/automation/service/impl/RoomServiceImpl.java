package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.RoomCreateRequest;
import com.hospital.automation.domain.dto.response.RoomResponse;
import com.hospital.automation.domain.entity.Room;
import com.hospital.automation.repository.RoomRepository;
import com.hospital.automation.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    public RoomResponse create(RoomCreateRequest request) {
        roomRepository.findByRoomNumber(request.roomNumber())
                .ifPresent(r -> { throw new BadRequestException("Room number already exists"); });

        Room room = Room.builder()
                .roomNumber(request.roomNumber())
                .floor(request.floor())
                .roomType(request.roomType())
                .capacity(request.capacity())
                .build();

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAll() {
        return roomRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found: " + id));
        return toResponse(room);
    }

    @Override
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new NotFoundException("Room not found: " + id);
        }
        roomRepository.deleteById(id);
    }

    private RoomResponse toResponse(Room r) {
        return new RoomResponse(r.getId(), r.getRoomNumber(), r.getFloor(), r.getRoomType(), r.getCapacity());
    }
}
