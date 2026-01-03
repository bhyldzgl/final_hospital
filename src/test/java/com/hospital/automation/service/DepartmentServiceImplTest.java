package com.hospital.automation.service;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.DepartmentCreateRequest;
import com.hospital.automation.domain.dto.response.DepartmentResponse;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    // ============= CREATE TESTLERİ =============

    @Test
    @DisplayName("create(): departman adı daha önce yoksa kaydeder ve response döner")
    void create_shouldSaveAndReturnResponse_whenNameNotExists() {
        // --- Arrange (Hazırlık) ---
        // Kullanıcıdan gelen istek
        DepartmentCreateRequest request = new DepartmentCreateRequest("Cardiology");

        // DB'de aynı isimde departman yokmuş gibi davran:
        when(departmentRepository.findByName("Cardiology"))
                .thenReturn(Optional.empty());

        // Repository save çağrılınca DB'nin id verdiğini varsayıyoruz:
        Department saved = Department.builder()
                .id(10L)
                .name("Cardiology")
                .build();

        // save(...) içine hangi Department gittiğini yakalamak için:
        when(departmentRepository.save(any(Department.class)))
                .thenReturn(saved);

        // --- Act (Çalıştırma) ---
        DepartmentResponse response = departmentService.create(request);

        // --- Assert (Doğrulama) ---
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Cardiology", response.name());

        // "findByName" gerçekten çağrıldı mı?
        verify(departmentRepository).findByName("Cardiology");

        // save(...) içine gönderilen Department doğru mu?
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());

        Department sentToSave = captor.getValue();
        assertNull(sentToSave.getId(), "Yeni oluşturulan entity'nin id'si save öncesi null olmalı");
        assertEquals("Cardiology", sentToSave.getName());

        // Ekstra başka repository çağrısı olmamalı:
        verifyNoMoreInteractions(departmentRepository);
    }

    @Test
    @DisplayName("create(): departman adı zaten varsa BadRequestException fırlatır")
    void create_shouldThrowBadRequest_whenDepartmentAlreadyExists() {
        // --- Arrange ---
        DepartmentCreateRequest request = new DepartmentCreateRequest("Cardiology");

        // DB'de bu isimde bir departman varmış gibi davran:
        Department existing = Department.builder().id(5L).name("Cardiology").build();
        when(departmentRepository.findByName("Cardiology"))
                .thenReturn(Optional.of(existing));

        // --- Act + Assert ---
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> departmentService.create(request)
        );
        assertEquals("Department already exists", ex.getMessage());

        // findByName çağrıldı mı?
        verify(departmentRepository).findByName("Cardiology");

        // Zaten var olduğu için save çağrılmamalı:
        verify(departmentRepository, never()).save(any());

        verifyNoMoreInteractions(departmentRepository);
    }

    // ============= GETALL TESTLERİ =============

    @Test
    @DisplayName("getAll(): repository'den gelen departmanları DepartmentResponse listesine çevirir")
    void getAll_shouldReturnMappedResponses() {
        // --- Arrange ---
        Department d1 = Department.builder().id(1L).name("Cardiology").build();
        Department d2 = Department.builder().id(2L).name("Neurology").build();

        when(departmentRepository.findAll())
                .thenReturn(List.of(d1, d2));

        // --- Act ---
        List<DepartmentResponse> result = departmentService.getAll();

        // --- Assert ---
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).id());
        assertEquals("Cardiology", result.get(0).name());

        assertEquals(2L, result.get(1).id());
        assertEquals("Neurology", result.get(1).name());

        verify(departmentRepository).findAll();
        verifyNoMoreInteractions(departmentRepository);
    }

    // ============= GETBYID TESTLERİ =============

    @Test
    @DisplayName("getById(): departman varsa response döner")
    void getById_shouldReturnResponse_whenExists() {
        // --- Arrange ---
        Department dept = Department.builder()
                .id(7L)
                .name("Radiology")
                .build();

        when(departmentRepository.findById(7L))
                .thenReturn(Optional.of(dept));

        // --- Act ---
        DepartmentResponse response = departmentService.getById(7L);

        // --- Assert ---
        assertNotNull(response);
        assertEquals(7L, response.id());
        assertEquals("Radiology", response.name());

        verify(departmentRepository).findById(7L);
        verifyNoMoreInteractions(departmentRepository);
    }

    @Test
    @DisplayName("getById(): departman yoksa NotFoundException fırlatır")
    void getById_shouldThrowNotFound_whenNotExists() {
        // --- Arrange ---
        when(departmentRepository.findById(99L))
                .thenReturn(Optional.empty());

        // --- Act + Assert ---
        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> departmentService.getById(99L)
        );
        assertEquals("Department not found: 99", ex.getMessage());

        verify(departmentRepository).findById(99L);
        verifyNoMoreInteractions(departmentRepository);
    }

    // ============= DELETE TESTLERİ =============

    @Test
    @DisplayName("delete(): departman varsa deleteById çağrılır")
    void delete_shouldDelete_whenExists() {
        // --- Arrange ---
        when(departmentRepository.existsById(15L))
                .thenReturn(true);

        // --- Act ---
        departmentService.delete(15L);

        // --- Assert ---
        verify(departmentRepository).existsById(15L);
        verify(departmentRepository).deleteById(15L);
        verifyNoMoreInteractions(departmentRepository);
    }

    @Test
    @DisplayName("delete(): departman yoksa NotFoundException fırlatır ve deleteById çağrılmaz")
    void delete_shouldThrowNotFound_whenNotExists() {
        // --- Arrange ---
        when(departmentRepository.existsById(15L))
                .thenReturn(false);

        // --- Act + Assert ---
        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> departmentService.delete(15L)
        );
        assertEquals("Department not found: 15", ex.getMessage());

        verify(departmentRepository).existsById(15L);

        // bulunamadığı için silme çağrılmamalı:
        verify(departmentRepository, never()).deleteById(anyLong());

        verifyNoMoreInteractions(departmentRepository);
    }
}
