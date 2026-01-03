package com.hospital.automation.service;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.AdmissionCreateRequest;
import com.hospital.automation.domain.dto.request.AdmissionDischargeRequest;
import com.hospital.automation.domain.dto.response.AdmissionResponse;
import com.hospital.automation.domain.entity.Admission;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.domain.entity.Room;
import com.hospital.automation.domain.enums.AdmissionStatus;
import com.hospital.automation.domain.enums.RoomType;
import com.hospital.automation.repository.AdmissionRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.repository.RoomRepository;
import com.hospital.automation.service.impl.AdmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BU DOSYA NEYİ TEST EDİYOR?
 * ------------------------------------------------------------
 * AdmissionServiceImpl içindeki iş kurallarını test eder.
 *
 * Unit test olduğu için:
 * - Gerçek DB yok
 * - Gerçek Spring Context yok
 * - Repository'ler ve AuditLogService MOCK
 *
 * Bu sayede sadece AdmissionServiceImpl'in doğru karar verip vermediğini kontrol ederiz.
 */
@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplTest {

    @Mock private AdmissionRepository admissionRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AdmissionServiceImpl admissionService;

    @Captor
    private ArgumentCaptor<Admission> admissionCaptor;

    private Patient patient;
    private Room room;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        // Testlerde tekrar tekrar kullanacağımız örnek nesneler:
        patient = Patient.builder()
                .id(1L)
                .firstName("Ali")
                .lastName("Veli")
                .build();

        room = Room.builder()
                .id(5L)
                .roomNumber("101A")
                .floor(1)
                .roomType(RoomType.WARD)
                .capacity(2)
                .build();

        doctor = Doctor.builder()
                .id(7L)
                .firstName("Ayşe")
                .lastName("Yılmaz")
                .specialization("Cardiology")
                .build();
    }

    // ------------------------------------------------------------
    // CREATE TESTLERİ
    // ------------------------------------------------------------

    @Test
    void create_shouldThrowNotFound_whenPatientNotExists() {
        // Amaç:
        // patient yoksa servis NotFoundException fırlatsın.
        // Çünkü service önce patient'i bulmaya çalışıyor.

        LocalDateTime admittedAt = LocalDateTime.now().plusDays(1);

        AdmissionCreateRequest request = new AdmissionCreateRequest(
                1L,     // patientId
                5L,     // roomId
                null,   // attendingDoctorId
                admittedAt,
                "note"
        );

        // Arrange (stub):
        // patient repo "empty" dönsün => patient bulunamasın
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act + Assert:
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> admissionService.create(request));

        assertTrue(ex.getMessage().contains("Patient not found"));

        // Önemli:
        // patient bulunamayınca servis daha ileri gitmemeli.
        verify(roomRepository, never()).findById(anyLong());
        verify(admissionRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void create_shouldThrowNotFound_whenRoomNotExists() {
        // Amaç:
        // room yoksa NotFoundException

        LocalDateTime admittedAt = LocalDateTime.now().plusDays(1);
        AdmissionCreateRequest request = new AdmissionCreateRequest(1L, 5L, null, admittedAt, null);

        // Arrange:
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(roomRepository.findById(5L)).thenReturn(Optional.empty());

        // Act + Assert:
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> admissionService.create(request));

        assertTrue(ex.getMessage().contains("Room not found"));

        // room bulunamayınca:
        verify(admissionRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void create_shouldThrowNotFound_whenAttendingDoctorIdProvided_butDoctorNotExists() {
        // Amaç:
        // attendingDoctorId null değilse doktor aranıyor.
        // doktor yoksa NotFoundException

        LocalDateTime admittedAt = LocalDateTime.now().plusDays(1);
        AdmissionCreateRequest request = new AdmissionCreateRequest(1L, 5L, 7L, admittedAt, "x");

        // Arrange:
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(doctorRepository.findById(7L)).thenReturn(Optional.empty());

        // Act + Assert:
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> admissionService.create(request));

        assertTrue(ex.getMessage().contains("Doctor not found"));

        verify(admissionRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void create_shouldThrowBadRequest_whenRoomCapacityReached() {
        // Amaç:
        // room.capacity != null ise aktif admission sayısı kontrol edilir.
        // activeCount >= capacity ise "Room is full" BadRequestException fırlatılır.

        LocalDateTime admittedAt = LocalDateTime.now().plusDays(1);
        AdmissionCreateRequest request = new AdmissionCreateRequest(1L, 5L, null, admittedAt, null);

        // Arrange:
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));

        // capacity = 2 (room içinde setUp'ta verdik)
        // aktif admission sayısı = 2 döndürürsek => dolu sayılır
        when(admissionRepository.countByRoomIdAndStatus(5L, AdmissionStatus.ADMITTED)).thenReturn(2L);

        // Act + Assert:
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> admissionService.create(request));

        assertTrue(ex.getMessage().contains("Room is full"));

        // Doluysa kayıt yapılmamalı:
        verify(admissionRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void create_shouldSaveAdmission_andLog_whenValid_andNoAttendingDoctor() {
        // Amaç:
        // Her şey uygunsa admission kaydedilmeli + audit log atılmalı
        // attendingDoctorId null ise doctor repository'ye hiç gidilmemeli

        LocalDateTime admittedAt = LocalDateTime.now().plusDays(1);
        AdmissionCreateRequest request = new AdmissionCreateRequest(1L, 5L, null, admittedAt, "note-1");

        // Arrange:
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(admissionRepository.countByRoomIdAndStatus(5L, AdmissionStatus.ADMITTED)).thenReturn(0L);

        // save() çağrılınca DB normalde id üretir. Unit testte DB yok.
        // Bu yüzden biz save() sonucu döndürülecek "saved" objesini kurgularız:
        when(admissionRepository.save(any(Admission.class))).thenAnswer(invocation -> {
            Admission a = invocation.getArgument(0);
            a.setId(100L); // id simüle
            return a;
        });

        // Act:
        AdmissionResponse response = admissionService.create(request);

        // Assert - Response kontrol:
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(AdmissionStatus.ADMITTED, response.status());

        assertEquals(patient.getId(), response.patient().id());
        assertEquals("Ali", response.patient().firstName());
        assertEquals("Veli", response.patient().lastName());

        assertEquals(room.getId(), response.room().id());
        assertEquals("101A", response.room().roomNumber());
        assertEquals(RoomType.WARD, response.room().roomType());
        assertEquals(2, response.room().capacity());

        // attendingDoctorId null => response.attendingDoctor null olmalı
        assertNull(response.attendingDoctor());

        // Save çağrısında hangi Admission gönderilmiş? (iş kuralları doğru mu?)
        verify(admissionRepository).save(admissionCaptor.capture());
        Admission savedEntity = admissionCaptor.getValue();

        assertEquals(patient, savedEntity.getPatient());
        assertEquals(room, savedEntity.getRoom());
        assertNull(savedEntity.getAttendingDoctor());
        assertEquals(admittedAt, savedEntity.getAdmittedAt());
        assertEquals(AdmissionStatus.ADMITTED, savedEntity.getStatus());
        assertEquals("note-1", savedEntity.getNote());

        // Doktor repo çağrılmamalı (çünkü id null)
        verify(doctorRepository, never()).findById(anyLong());

        // Audit log atılmalı
        verify(auditLogService).log(
                eq("CREATE"),
                eq("Admission"),
                eq(100L),
                contains("Admission created")
        );
    }

    @Test
    void create_shouldSaveAdmission_andLog_whenValid_andAttendingDoctorProvided() {
        // Amaç:
        // attendingDoctorId verilmişse doctor bulunup admission'a set edilmeli

        LocalDateTime admittedAt = LocalDateTime.now().plusDays(1);
        AdmissionCreateRequest request = new AdmissionCreateRequest(1L, 5L, 7L, admittedAt, "note-2");

        // Arrange:
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(doctorRepository.findById(7L)).thenReturn(Optional.of(doctor));
        when(admissionRepository.countByRoomIdAndStatus(5L, AdmissionStatus.ADMITTED)).thenReturn(0L);

        when(admissionRepository.save(any(Admission.class))).thenAnswer(invocation -> {
            Admission a = invocation.getArgument(0);
            a.setId(101L);
            return a;
        });

        // Act:
        AdmissionResponse response = admissionService.create(request);

        // Assert:
        assertEquals(101L, response.id());
        assertNotNull(response.attendingDoctor());
        assertEquals(doctor.getId(), response.attendingDoctor().id());
        assertEquals("Ayşe", response.attendingDoctor().firstName());

        verify(admissionRepository).save(admissionCaptor.capture());
        Admission savedEntity = admissionCaptor.getValue();
        assertEquals(doctor, savedEntity.getAttendingDoctor());

        verify(auditLogService).log(eq("CREATE"), eq("Admission"), eq(101L), anyString());
    }

    // ------------------------------------------------------------
    // GET TESTLERİ
    // ------------------------------------------------------------

    @Test
    void getById_shouldThrowNotFound_whenAdmissionNotExists() {
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> admissionService.getById(999L));

        assertTrue(ex.getMessage().contains("Admission not found"));
    }

    @Test
    void getById_shouldReturnResponse_whenExists() {
        Admission admission = Admission.builder()
                .id(200L)
                .patient(patient)
                .room(room)
                .attendingDoctor(doctor)
                .admittedAt(LocalDateTime.now().plusDays(1))
                .status(AdmissionStatus.ADMITTED)
                .note("hello")
                .build();

        when(admissionRepository.findById(200L)).thenReturn(Optional.of(admission));

        AdmissionResponse response = admissionService.getById(200L);

        assertEquals(200L, response.id());
        assertEquals(AdmissionStatus.ADMITTED, response.status());
        assertEquals(patient.getId(), response.patient().id());
        assertEquals(room.getId(), response.room().id());
        assertNotNull(response.attendingDoctor());
        assertEquals(doctor.getId(), response.attendingDoctor().id());
    }

    @Test
    void getAll_shouldMapEntities_toResponses() {
        Admission a1 = Admission.builder()
                .id(1L)
                .patient(patient)
                .room(room)
                .admittedAt(LocalDateTime.now().plusDays(1))
                .status(AdmissionStatus.ADMITTED)
                .build();

        Admission a2 = Admission.builder()
                .id(2L)
                .patient(patient)
                .room(room)
                .admittedAt(LocalDateTime.now().plusDays(2))
                .status(AdmissionStatus.ADMITTED)
                .build();

        when(admissionRepository.findAll()).thenReturn(List.of(a1, a2));

        List<AdmissionResponse> list = admissionService.getAll();

        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).id());
        assertEquals(2L, list.get(1).id());
    }

    // ------------------------------------------------------------
    // DISCHARGE TESTLERİ
    // ------------------------------------------------------------

    @Test
    void discharge_shouldThrowNotFound_whenAdmissionNotExists() {
        when(admissionRepository.findById(404L)).thenReturn(Optional.empty());

        AdmissionDischargeRequest request = new AdmissionDischargeRequest(LocalDateTime.now().plusDays(1), "x");

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> admissionService.discharge(404L, request));

        assertTrue(ex.getMessage().contains("Admission not found"));
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void discharge_shouldThrowBadRequest_whenAlreadyDischarged() {
        Admission admission = Admission.builder()
                .id(10L)
                .patient(patient)
                .room(room)
                .admittedAt(LocalDateTime.now().minusDays(1))
                .dischargedAt(LocalDateTime.now())
                .status(AdmissionStatus.DISCHARGED)
                .build();

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));

        AdmissionDischargeRequest request = new AdmissionDischargeRequest(LocalDateTime.now().plusHours(1), "x");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> admissionService.discharge(10L, request));

        assertTrue(ex.getMessage().contains("already discharged"));
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void discharge_shouldThrowBadRequest_whenDischargedAtBeforeAdmittedAt() {
        LocalDateTime admittedAt = LocalDateTime.now().plusDays(2);
        Admission admission = Admission.builder()
                .id(11L)
                .patient(patient)
                .room(room)
                .admittedAt(admittedAt)
                .status(AdmissionStatus.ADMITTED)
                .build();

        when(admissionRepository.findById(11L)).thenReturn(Optional.of(admission));

        // dischargedAt admittedAt'tan önce => hata
        AdmissionDischargeRequest request = new AdmissionDischargeRequest(admittedAt.minusHours(1), "x");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> admissionService.discharge(11L, request));

        assertTrue(ex.getMessage().contains("dischargedAt cannot be before admittedAt"));
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void discharge_shouldUpdateStatusAndTime_andLog_whenValid_withNote() {
        LocalDateTime admittedAt = LocalDateTime.now().minusDays(1);

        Admission admission = Admission.builder()
                .id(12L)
                .patient(patient)
                .room(room)
                .admittedAt(admittedAt)
                .status(AdmissionStatus.ADMITTED)
                .note("old-note")
                .build();

        when(admissionRepository.findById(12L)).thenReturn(Optional.of(admission));

        LocalDateTime dischargedAt = LocalDateTime.now();
        AdmissionDischargeRequest request = new AdmissionDischargeRequest(dischargedAt, "new-note");

        AdmissionResponse response = admissionService.discharge(12L, request);

        // Entity güncellenmiş mi?
        assertEquals(AdmissionStatus.DISCHARGED, admission.getStatus());
        assertEquals(dischargedAt, admission.getDischargedAt());
        assertEquals("new-note", admission.getNote());

        // Response doğru mu?
        assertEquals(AdmissionStatus.DISCHARGED, response.status());
        assertEquals(dischargedAt, response.dischargedAt());
        assertEquals("new-note", response.note());

        // Audit log atıldı mı?
        verify(auditLogService).log(eq("DISCHARGE"), eq("Admission"), eq(12L), contains("discharged"));
    }

    @Test
    void discharge_shouldNotOverrideNote_whenRequestNoteIsNull() {
        // Amaç:
        // Kodda şu var:
        // if (request.note() != null) a.setNote(request.note());
        // yani request.note null ise eski not korunmalı.

        LocalDateTime admittedAt = LocalDateTime.now().minusDays(1);

        Admission admission = Admission.builder()
                .id(13L)
                .patient(patient)
                .room(room)
                .admittedAt(admittedAt)
                .status(AdmissionStatus.ADMITTED)
                .note("keep-this-note")
                .build();

        when(admissionRepository.findById(13L)).thenReturn(Optional.of(admission));

        LocalDateTime dischargedAt = LocalDateTime.now();
        AdmissionDischargeRequest request = new AdmissionDischargeRequest(dischargedAt, null);

        AdmissionResponse response = admissionService.discharge(13L, request);

        assertEquals(AdmissionStatus.DISCHARGED, admission.getStatus());
        assertEquals("keep-this-note", admission.getNote());
        assertEquals("keep-this-note", response.note());

        verify(auditLogService).log(eq("DISCHARGE"), eq("Admission"), eq(13L), anyString());
    }

    // ------------------------------------------------------------
    // DELETE TESTLERİ
    // ------------------------------------------------------------

    @Test
    void delete_shouldThrowNotFound_whenAdmissionNotExists() {
        when(admissionRepository.findById(888L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> admissionService.delete(888L));

        verify(admissionRepository, never()).delete(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void delete_shouldDeleteAndLog_whenExists() {
        Admission admission = Admission.builder()
                .id(300L)
                .patient(patient)
                .room(room)
                .admittedAt(LocalDateTime.now().minusDays(1))
                .status(AdmissionStatus.ADMITTED)
                .build();

        when(admissionRepository.findById(300L)).thenReturn(Optional.of(admission));

        admissionService.delete(300L);

        // delete gerçekten çağrıldı mı?
        verify(admissionRepository).delete(admission);

        // audit log çağrıldı mı?
        verify(auditLogService).log(eq("DELETE"), eq("Admission"), eq(300L), contains("deleted"));
    }
}
