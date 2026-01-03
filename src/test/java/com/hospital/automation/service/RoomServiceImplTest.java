package com.hospital.automation.service;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.RoomCreateRequest;
import com.hospital.automation.domain.dto.response.RoomResponse;
import com.hospital.automation.domain.entity.Room;
import com.hospital.automation.domain.enums.RoomType;
import com.hospital.automation.repository.RoomRepository;
import com.hospital.automation.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    @Test
    void create_shouldThrowBadRequest_whenRoomNumberAlreadyExists() {
        // given
        RoomCreateRequest req = new RoomCreateRequest("101A", 1, RoomType.WARD, 2);

        when(roomRepository.findByRoomNumber("101A"))
                .thenReturn(Optional.of(Room.builder().id(99L).roomNumber("101A").build()));

        // when + then
        BadRequestException ex = assertThrows(BadRequestException.class, () -> roomService.create(req));
        assertEquals("Room number already exists", ex.getMessage());

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void create_shouldSaveRoom_andReturnResponse() {
        // given
        RoomCreateRequest req = new RoomCreateRequest("101A", 1, RoomType.WARD, 2);

        when(roomRepository.findByRoomNumber("101A")).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        // when
        RoomResponse res = roomService.create(req);

        // then
        assertNotNull(res);
        assertEquals(10L, res.id());
        assertEquals("101A", res.roomNumber());
        assertEquals(1, res.floor());
        assertEquals(RoomType.WARD, res.roomType());
        assertEquals(2, res.capacity());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());

        Room savedArg = captor.getValue();
        assertEquals("101A", savedArg.getRoomNumber());
        assertEquals(1, savedArg.getFloor());
        assertEquals(RoomType.WARD, savedArg.getRoomType());
        assertEquals(2, savedArg.getCapacity());
    }

    @Test
    void getAll_shouldReturnMappedList() {
        // given
        when(roomRepository.findAll()).thenReturn(List.of(
                Room.builder().id(1L).roomNumber("101A").floor(1).roomType(RoomType.WARD).capacity(2).build(),
                Room.builder().id(2L).roomNumber("ICU-1").floor(2).roomType(RoomType.ICU).capacity(1).build()
        ));

        // when
        List<RoomResponse> list = roomService.getAll();

        // then
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).id());
        assertEquals("101A", list.get(0).roomNumber());
        assertEquals(RoomType.ICU, list.get(1).roomType());
    }

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        // given
        when(roomRepository.findById(777L)).thenReturn(Optional.empty());

        // when + then
        NotFoundException ex = assertThrows(NotFoundException.class, () -> roomService.getById(777L));
        assertEquals("Room not found: 777", ex.getMessage());
    }

    @Test
    void getById_shouldReturnResponse_whenExists() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(
                Room.builder().id(1L).roomNumber("101A").floor(1).roomType(RoomType.WARD).capacity(2).build()
        ));

        // when
        RoomResponse res = roomService.getById(1L);

        // then
        assertEquals(1L, res.id());
        assertEquals("101A", res.roomNumber());
        assertEquals(1, res.floor());
        assertEquals(RoomType.WARD, res.roomType());
        assertEquals(2, res.capacity());
    }

    @Test
    void delete_shouldThrowNotFound_whenRoomNotExists() {
        // given
        when(roomRepository.existsById(10L)).thenReturn(false);

        // when + then
        NotFoundException ex = assertThrows(NotFoundException.class, () -> roomService.delete(10L));
        assertEquals("Room not found: 10", ex.getMessage());

        verify(roomRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_shouldDeleteById_whenExists() {
        // given
        when(roomRepository.existsById(10L)).thenReturn(true);

        // when
        roomService.delete(10L);

        // then
        verify(roomRepository).deleteById(10L);
    }
}
