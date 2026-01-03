package com.hospital.automation.service;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.DoctorCreateRequest;
import com.hospital.automation.domain.dto.response.DoctorResponse;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
class DoctorServiceImplTest {

    // DoctorServiceImpl'in kullandığı bağımlılıkları mock'luyoruz.
    // Çünkü bu bir UNIT test: DB'ye gerçekten gitmek istemiyoruz.
    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    // Test edeceğimiz gerçek sınıf (içine mock'lar enjekte edilecek)
    @InjectMocks
    private DoctorServiceImpl doctorService;

    private Department cardiology;
    private Doctor savedDoctorWithDept;
    private Doctor savedDoctorWithoutDept;

    @BeforeEach
    void setUp() {
        cardiology = Department.builder()
                .id(1L)
                .name("Cardiology")
                .build();

        savedDoctorWithDept = Doctor.builder()
                .id(10L)
                .firstName("Ahmet")
                .lastName("Yilmaz")
                .specialization("Cardiologist")
                .department(cardiology)
                .build();

        savedDoctorWithoutDept = Doctor.builder()
                .id(11L)
                .firstName("Ayse")
                .lastName("Demir")
                .specialization("General")
                .department(null)
                .build();
    }

    // ------------------------------------------------------------
    // CREATE TESTLERİ
    // ------------------------------------------------------------

    @Test
    @DisplayName("create: departmentId NULL ise doctor department NULL kaydedilir ve response'da department NULL döner")
    void create_shouldCreateDoctor_withoutDepartment_whenDepartmentIdIsNull() {
        // 1) Arrange (Hazırlık)
        // departmentId = null => servis DepartmentRepository'e gitmeyecek
        DoctorCreateRequest request = new DoctorCreateRequest(
                "Ayse", "Demir", "General", null
        );

        // doctorRepository.save(...) çağrılınca DB'ye yazılmış gibi "savedDoctorWithoutDept" dönsün diyoruz
        when(doctorRepository.save(any(Doctor.class))).thenReturn(savedDoctorWithoutDept);

        // 2) Act (Çalıştır)
        DoctorResponse response = doctorService.create(request);

        // 3) Assert (Doğrula)
        assertNotNull(response);
        assertEquals(11L, response.id());
        assertEquals("Ayse", response.firstName());
        assertEquals("Demir", response.lastName());
        assertEquals("General", response.specialization());
        assertNull(response.department()); // departmentId null olduğu için response.department null olmalı

        // 4) Davranış doğrulama (Mock'lara nasıl gidildi?)
        // DepartmentRepository hiç çağrılmamalı:
        verifyNoInteractions(departmentRepository);

        // DoctorRepository.save 1 kere çağrılmalı:
        verify(doctorRepository, times(1)).save(any(Doctor.class));
    }

    @Test
    @DisplayName("create: departmentId dolu ise department bulunur, doctor'a set edilir, response'da department dolu döner")
    void create_shouldCreateDoctor_withDepartment_whenDepartmentIdProvided() {
        // 1) Arrange
        Long deptId = 1L;
        DoctorCreateRequest request = new DoctorCreateRequest(
                "Ahmet", "Yilmaz", "Cardiologist", deptId
        );

        // department bulundu
        when(departmentRepository.findById(deptId)).thenReturn(Optional.of(cardiology));

        // doctor kaydedildi
        when(doctorRepository.save(any(Doctor.class))).thenReturn(savedDoctorWithDept);

        // save'e giden Doctor objesinin içini kontrol edebilmek için captor kullanıyoruz
        ArgumentCaptor<Doctor> doctorCaptor = ArgumentCaptor.forClass(Doctor.class);

        // 2) Act
        DoctorResponse response = doctorService.create(request);

        // 3) Assert (response)
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Ahmet", response.firstName());
        assertEquals("Yilmaz", response.lastName());
        assertEquals("Cardiologist", response.specialization());
        assertNotNull(response.department());
        assertEquals(1L, response.department().id());
        assertEquals("Cardiology", response.department().name());

        // 4) Assert (davranış)
        verify(departmentRepository, times(1)).findById(deptId);

        verify(doctorRepository).save(doctorCaptor.capture());
        Doctor doctorSentToSave = doctorCaptor.getValue();

        // Servis doctor'a department'i gerçekten set etmiş mi?
        assertNotNull(doctorSentToSave.getDepartment());
        assertEquals(1L, doctorSentToSave.getDepartment().getId());
    }

    @Test
    @DisplayName("create: departmentId verilmiş ama department yoksa NotFoundException fırlatır ve save çağrılmaz")
    void create_shouldThrowNotFound_whenDepartmentNotExists() {
        // 1) Arrange
        Long deptId = 999L;
        DoctorCreateRequest request = new DoctorCreateRequest(
                "Ahmet", "Yilmaz", "Cardiologist", deptId
        );

        when(departmentRepository.findById(deptId)).thenReturn(Optional.empty());

        // 2) Act + Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () -> doctorService.create(request));
        assertTrue(ex.getMessage().contains("Department not found: " + deptId));

        // department'e baktıktan sonra patlayacağı için save'e hiç gitmemeli:
        verify(departmentRepository, times(1)).findById(deptId);
        verifyNoInteractions(doctorRepository);
    }

    // ------------------------------------------------------------
    // GET ALL TESTLERİ
    // ------------------------------------------------------------

    @Test
    @DisplayName("getAll: repository'den gelen doctor listesini response listesine map eder")
    void getAll_shouldReturnMappedDoctorResponses() {
        // 1) Arrange
        when(doctorRepository.findAll()).thenReturn(List.of(savedDoctorWithDept, savedDoctorWithoutDept));

        // 2) Act
        List<DoctorResponse> responses = doctorService.getAll();

        // 3) Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());

        DoctorResponse r1 = responses.get(0);
        assertEquals(10L, r1.id());
        assertNotNull(r1.department());
        assertEquals("Cardiology", r1.department().name());

        DoctorResponse r2 = responses.get(1);
        assertEquals(11L, r2.id());
        assertNull(r2.department());

        verify(doctorRepository, times(1)).findAll();
        verifyNoInteractions(departmentRepository); // getAll içinde dept repo kullanılmıyor
    }

    // ------------------------------------------------------------
    // GET BY ID TESTLERİ
    // ------------------------------------------------------------

    @Test
    @DisplayName("getById: doctor varsa response döner")
    void getById_shouldReturnDoctor_whenExists() {
        // 1) Arrange
        Long id = 10L;
        when(doctorRepository.findById(id)).thenReturn(Optional.of(savedDoctorWithDept));

        // 2) Act
        DoctorResponse response = doctorService.getById(id);

        // 3) Assert
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Ahmet", response.firstName());

        verify(doctorRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("getById: doctor yoksa NotFoundException fırlatır")
    void getById_shouldThrowNotFound_whenDoctorNotExists() {
        // 1) Arrange
        Long id = 404L;
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        // 2) Act + Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () -> doctorService.getById(id));
        assertTrue(ex.getMessage().contains("Doctor not found: " + id));

        verify(doctorRepository, times(1)).findById(id);
    }

    // ------------------------------------------------------------
    // DELETE TESTLERİ
    // ------------------------------------------------------------

    @Test
    @DisplayName("delete: doctor yoksa NotFoundException fırlatır ve deleteById çağrılmaz")
    void delete_shouldThrowNotFound_whenDoctorNotExists() {
        // 1) Arrange
        Long id = 123L;
        when(doctorRepository.existsById(id)).thenReturn(false);

        // 2) Act + Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () -> doctorService.delete(id));
        assertTrue(ex.getMessage().contains("Doctor not found: " + id));

        // 3) verify
        verify(doctorRepository, times(1)).existsById(id);
        verify(doctorRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete: doctor varsa deleteById çağrılır")
    void delete_shouldDelete_whenDoctorExists() {
        // 1) Arrange
        Long id = 10L;
        when(doctorRepository.existsById(id)).thenReturn(true);

        // 2) Act
        assertDoesNotThrow(() -> doctorService.delete(id));

        // 3) verify
        verify(doctorRepository, times(1)).existsById(id);
        verify(doctorRepository, times(1)).deleteById(id);
    }
}
