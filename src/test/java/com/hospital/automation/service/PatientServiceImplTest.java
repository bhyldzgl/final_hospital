package com.hospital.automation.service;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.PatientCreateRequest;
import com.hospital.automation.domain.dto.request.PatientUpdateRequest;
import com.hospital.automation.domain.dto.response.PatientResponse;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient patient1;
    private Patient patient2;

    @BeforeEach
    void setUp() {
        patient1 = Patient.builder()
                .id(10L)
                .firstName("Ali")
                .lastName("Veli")
                .birthDate(LocalDate.of(1999, 1, 1))
                .nationalId("11111111111")
                .phone("5551112233")
                .address("Istanbul")
                .build();

        patient2 = Patient.builder()
                .id(20L)
                .firstName("Ayse")
                .lastName("Yilmaz")
                .birthDate(LocalDate.of(2000, 2, 2))
                .nationalId("22222222222")
                .phone("5554445566")
                .address("Ankara")
                .build();
    }

    // ---------------------------
    // CREATE
    // ---------------------------

    @Test
    void create_shouldSaveAndReturnResponse_whenValid_withNationalId() {
        PatientCreateRequest req = new PatientCreateRequest(
                "Ali",
                "Veli",
                LocalDate.of(1999, 1, 1),
                "11111111111",
                "5551112233",
                "Istanbul"
        );

        when(patientRepository.findByNationalId("11111111111")).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenReturn(patient1);

        PatientResponse res = patientService.create(req);

        assertNotNull(res);
        assertEquals(10L, res.id());
        assertEquals("Ali", res.firstName());
        assertEquals("Veli", res.lastName());
        assertEquals("11111111111", res.nationalId());

        // save argümanını kontrol edelim
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        Patient savedArg = captor.getValue();
        assertEquals("Ali", savedArg.getFirstName());
        assertEquals("Veli", savedArg.getLastName());
        assertEquals("11111111111", savedArg.getNationalId());

        verify(auditLogService).log(
                eq("CREATE"),
                eq("Patient"),
                eq(10L),
                contains("Patient created:")
        );
    }

    @Test
    void create_shouldSaveAndReturnResponse_whenNationalIdNull_orBlank_shouldNotCheckUniqueness() {
        PatientCreateRequest req = new PatientCreateRequest(
                "Ali",
                "Veli",
                LocalDate.of(1999, 1, 1),
                "   ", // blank
                "5551112233",
                "Istanbul"
        );

        when(patientRepository.save(any(Patient.class))).thenReturn(patient1);

        PatientResponse res = patientService.create(req);

        assertEquals(10L, res.id());

        // nationalId blank olduğu için findByNationalId çağrılmamalı
        verify(patientRepository, never()).findByNationalId(anyString());

        verify(auditLogService).log(
                eq("CREATE"),
                eq("Patient"),
                eq(10L),
                contains("Patient created:")
        );
    }

    @Test
    void create_shouldThrowBadRequest_whenNationalIdAlreadyExists() {
        PatientCreateRequest req = new PatientCreateRequest(
                "Ali",
                "Veli",
                LocalDate.of(1999, 1, 1),
                "11111111111",
                "5551112233",
                "Istanbul"
        );

        when(patientRepository.findByNationalId("11111111111")).thenReturn(Optional.of(patient2));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> patientService.create(req));
        assertEquals("nationalId already exists", ex.getMessage());

        verify(patientRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    // ---------------------------
    // GET ALL
    // ---------------------------

    @Test
    void getAll_shouldReturnMappedList() {
        when(patientRepository.findAll()).thenReturn(List.of(patient1, patient2));

        List<PatientResponse> list = patientService.getAll();

        assertEquals(2, list.size());
        assertEquals(10L, list.get(0).id());
        assertEquals(20L, list.get(1).id());
        assertEquals("Ayse", list.get(1).firstName());

        verify(patientRepository).findAll();
        verifyNoInteractions(auditLogService);
    }

    // ---------------------------
    // GET BY ID
    // ---------------------------

    @Test
    void getById_shouldReturnMapped_whenExists() {
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient1));

        PatientResponse res = patientService.getById(10L);

        assertEquals(10L, res.id());
        assertEquals("Ali", res.firstName());

        verify(patientRepository).findById(10L);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void getById_shouldThrowNotFound_whenNotExists() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> patientService.getById(99L));
        assertEquals("Patient not found: 99", ex.getMessage());

        verify(patientRepository).findById(99L);
        verifyNoInteractions(auditLogService);
    }

    // ---------------------------
    // UPDATE
    // ---------------------------

    @Test
    void update_shouldThrowNotFound_whenPatientNotExists() {
        PatientUpdateRequest req = new PatientUpdateRequest(
                "New",
                "Name",
                LocalDate.of(1990, 1, 1),
                "99999999999",
                "5550000000",
                "New Address"
        );

        when(patientRepository.findById(77L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> patientService.update(77L, req));
        assertEquals("Patient not found: 77", ex.getMessage());

        verify(patientRepository).findById(77L);
        verify(patientRepository, never()).findByNationalId(anyString());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void update_shouldThrowBadRequest_whenNationalIdBelongsToAnotherPatient() {
        // update edilen kişi 10L
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient1));
        // yeni nationalId başka bir hastaya ait (id=20)
        when(patientRepository.findByNationalId("22222222222")).thenReturn(Optional.of(patient2));

        PatientUpdateRequest req = new PatientUpdateRequest(
                "Ali",
                "Veli",
                LocalDate.of(1999, 1, 1),
                "22222222222",
                "5551112233",
                "Istanbul"
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> patientService.update(10L, req));
        assertEquals("nationalId already exists", ex.getMessage());

        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void update_shouldUpdateFieldsAndReturnResponse_whenValid_andSameNationalIdOwner() {
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient1));
        // aynı patient dönüyor (id=10) -> filter(!equals(id)) ile elenecek, hata yok
        when(patientRepository.findByNationalId("11111111111")).thenReturn(Optional.of(patient1));

        PatientUpdateRequest req = new PatientUpdateRequest(
                "AliUpdated",
                "VeliUpdated",
                LocalDate.of(1998, 12, 31),
                "11111111111",
                "5559998887",
                "Izmir"
        );

        PatientResponse res = patientService.update(10L, req);

        assertEquals(10L, res.id());
        assertEquals("AliUpdated", res.firstName());
        assertEquals("VeliUpdated", res.lastName());
        assertEquals("Izmir", res.address());

        // Entity üzerinde set’ler çalıştı mı?
        assertEquals("AliUpdated", patient1.getFirstName());
        assertEquals("VeliUpdated", patient1.getLastName());
        assertEquals(LocalDate.of(1998, 12, 31), patient1.getBirthDate());
        assertEquals("11111111111", patient1.getNationalId());
        assertEquals("5559998887", patient1.getPhone());
        assertEquals("Izmir", patient1.getAddress());

        verify(auditLogService).log(
                eq("UPDATE"),
                eq("Patient"),
                eq(10L),
                contains("Patient updated:")
        );
        // update’te repo.save çağrısı yok (serviste yok). O yüzden verify(save) yapmıyoruz.
        verify(patientRepository).findById(10L);
        verify(patientRepository).findByNationalId("11111111111");
    }

    @Test
    void update_shouldNotCheckUniqueness_whenNationalIdNullOrBlank() {
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient1));

        PatientUpdateRequest req = new PatientUpdateRequest(
                "AliUpdated",
                "VeliUpdated",
                LocalDate.of(1998, 12, 31),
                "   ", // blank => kontrol yok
                "5559998887",
                "Izmir"
        );

        PatientResponse res = patientService.update(10L, req);

        assertEquals("AliUpdated", res.firstName());
        verify(patientRepository, never()).findByNationalId(anyString());

        verify(auditLogService).log(
                eq("UPDATE"),
                eq("Patient"),
                eq(10L),
                contains("Patient updated:")
        );
    }

    // ---------------------------
    // DELETE
    // ---------------------------

    @Test
    void delete_shouldThrowNotFound_whenNotExists() {
        when(patientRepository.findById(55L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> patientService.delete(55L));
        assertEquals("Patient not found: 55", ex.getMessage());

        verify(patientRepository).findById(55L);
        verify(patientRepository, never()).delete(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void delete_shouldDeleteAndLog_whenExists() {
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient1));

        patientService.delete(10L);

        verify(patientRepository).delete(patient1);

        verify(auditLogService).log(
                eq("DELETE"),
                eq("Patient"),
                eq(10L),
                contains("Patient deleted:")
        );
    }
}
