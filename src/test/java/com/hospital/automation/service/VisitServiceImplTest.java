package com.hospital.automation.service;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.VisitCreateRequest;
import com.hospital.automation.domain.dto.response.VisitResponse;
import com.hospital.automation.domain.entity.*;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.repository.VisitRepository;
import com.hospital.automation.service.impl.VisitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceImplTest {

    @Mock private VisitRepository visitRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private VisitServiceImpl visitService;

    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(10L)
                .firstName("Ali")
                .lastName("Veli")
                .build();

        doctor = Doctor.builder()
                .id(20L)
                .firstName("Ayşe")
                .lastName("Yılmaz")
                .specialization("Cardiology")
                .build();
    }

    @Test
    void create_shouldCreateVisit_whenAppointmentIsNull() {
        LocalDateTime vt = LocalDateTime.of(2026, 1, 3, 10, 0);

        VisitCreateRequest req = new VisitCreateRequest(
                patient.getId(),
                doctor.getId(),
                null,
                vt,
                "Headache",
                "Migraine"
        );

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        // save() dönüşünü kontrol edebilmek için id set edelim
        Visit saved = Visit.builder()
                .id(100L)
                .patient(patient)
                .doctor(doctor)
                .appointment(null)
                .visitTime(vt)
                .complaint("Headache")
                .diagnosis("Migraine")
                .medicalRecords(new ArrayList<>())
                .prescriptions(new ArrayList<>())
                .build();

        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        when(visitRepository.save(visitCaptor.capture())).thenReturn(saved);

        VisitResponse res = visitService.create(req);

        // save'a giden entity doğru mu?
        Visit toSave = visitCaptor.getValue();
        assertNotNull(toSave);
        assertEquals(patient, toSave.getPatient());
        assertEquals(doctor, toSave.getDoctor());
        assertNull(toSave.getAppointment());
        assertEquals(vt, toSave.getVisitTime());
        assertEquals("Headache", toSave.getComplaint());
        assertEquals("Migraine", toSave.getDiagnosis());

        // response mapping
        assertEquals(100L, res.id());
        assertEquals(10L, res.patient().id());
        assertEquals("Ali", res.patient().firstName());
        assertEquals("Veli", res.patient().lastName());

        assertEquals(20L, res.doctor().id());
        assertEquals("Ayşe", res.doctor().firstName());
        assertEquals("Yılmaz", res.doctor().lastName());
        assertEquals("Cardiology", res.doctor().specialization());

        assertNull(res.appointmentId());
        assertEquals(vt, res.visitTime());
        assertEquals("Headache", res.complaint());
        assertEquals("Migraine", res.diagnosis());
        assertNotNull(res.medicalRecords());
        assertNotNull(res.prescriptions());
        assertEquals(0, res.medicalRecords().size());
        assertEquals(0, res.prescriptions().size());

        // audit log doğrulama
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService, times(1)).log(
                eq("CREATE"),
                eq("Visit"),
                eq(100L),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertTrue(details.contains("patientId=10"));
        assertTrue(details.contains("doctorId=20"));
        assertFalse(details.contains("appointmentId="));
    }

    @Test
    void create_shouldCreateVisit_whenAppointmentProvided() {
        LocalDateTime vt = LocalDateTime.of(2026, 1, 3, 11, 0);

        Appointment appointment = Appointment.builder()
                .id(30L)
                .patient(patient)
                .doctor(doctor)
                .startTime(vt.minusMinutes(30))
                .endTime(vt.minusMinutes(10))
                .build();

        VisitCreateRequest req = new VisitCreateRequest(
                patient.getId(),
                doctor.getId(),
                appointment.getId(),
                vt,
                "Cough",
                "Flu"
        );

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        Visit saved = Visit.builder()
                .id(101L)
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .visitTime(vt)
                .complaint("Cough")
                .diagnosis("Flu")
                .medicalRecords(new ArrayList<>())
                .prescriptions(new ArrayList<>())
                .build();

        when(visitRepository.save(any(Visit.class))).thenReturn(saved);

        VisitResponse res = visitService.create(req);

        assertEquals(101L, res.id());
        assertEquals(30L, res.appointmentId());

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq("CREATE"), eq("Visit"), eq(101L), detailsCaptor.capture());

        String details = detailsCaptor.getValue();
        assertTrue(details.contains("patientId=10"));
        assertTrue(details.contains("doctorId=20"));
        assertTrue(details.contains("appointmentId=30"));
    }

    @Test
    void create_shouldThrowNotFound_whenPatientMissing() {
        VisitCreateRequest req = new VisitCreateRequest(
                999L, doctor.getId(), null,
                LocalDateTime.now(), null, null
        );

        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> visitService.create(req));
        assertTrue(ex.getMessage().contains("Patient not found"));

        verify(visitRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void create_shouldThrowNotFound_whenDoctorMissing() {
        VisitCreateRequest req = new VisitCreateRequest(
                patient.getId(), 999L, null,
                LocalDateTime.now(), null, null
        );

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> visitService.create(req));
        assertTrue(ex.getMessage().contains("Doctor not found"));

        verify(visitRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void create_shouldThrowNotFound_whenAppointmentProvidedButMissing() {
        VisitCreateRequest req = new VisitCreateRequest(
                patient.getId(), doctor.getId(), 999L,
                LocalDateTime.now(), null, null
        );

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> visitService.create(req));
        assertTrue(ex.getMessage().contains("Appointment not found"));

        verify(visitRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }

    @Test
    void getAll_shouldReturnMappedResponses_withNestedLists() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 3, 12, 0);

        // medical record
        MedicalRecord mr = MedicalRecord.builder()
                .id(501L)
                .recordType("LAB")
                .content("Hb: 13.2")
                .createdAt(now.minusHours(1))
                .build();

        // prescription + item
        PrescriptionItem pi = PrescriptionItem.builder()
                .id(701L)
                .drugName("Paracetamol")
                .dosage("500mg")
                .frequency("2x/day")
                .durationDays(3)
                .instructions("After meal")
                .build();

        Prescription pr = Prescription.builder()
                .id(601L)
                .createdAt(now.minusMinutes(30))
                .note("Take care")
                .items(new ArrayList<>())
                .build();
        pr.getItems().add(pi);

        Visit v = Visit.builder()
                .id(200L)
                .patient(patient)
                .doctor(doctor)
                .appointment(null)
                .visitTime(now)
                .complaint("Pain")
                .diagnosis("Check")
                .medicalRecords(new ArrayList<>())
                .prescriptions(new ArrayList<>())
                .build();
        v.getMedicalRecords().add(mr);
        v.getPrescriptions().add(pr);

        when(visitRepository.findAll()).thenReturn(List.of(v));

        List<VisitResponse> list = visitService.getAll();

        assertEquals(1, list.size());
        VisitResponse res = list.get(0);

        assertEquals(200L, res.id());
        assertEquals(10L, res.patient().id());
        assertEquals(20L, res.doctor().id());
        assertNull(res.appointmentId());

        assertEquals(1, res.medicalRecords().size());
        assertEquals("LAB", res.medicalRecords().get(0).recordType());
        assertEquals("Hb: 13.2", res.medicalRecords().get(0).content());

        assertEquals(1, res.prescriptions().size());
        assertEquals("Take care", res.prescriptions().get(0).note());
        assertEquals(1, res.prescriptions().get(0).items().size());
        assertEquals("Paracetamol", res.prescriptions().get(0).items().get(0).drugName());
        assertEquals("500mg", res.prescriptions().get(0).items().get(0).dosage());
        assertEquals("2x/day", res.prescriptions().get(0).items().get(0).frequency());
        assertEquals(3, res.prescriptions().get(0).items().get(0).durationDays());
        assertEquals("After meal", res.prescriptions().get(0).items().get(0).instructions());
    }

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(visitRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> visitService.getById(999L));
        assertTrue(ex.getMessage().contains("Visit not found"));
    }

    @Test
    void delete_shouldDeleteAndLog_whenExists() {
        Visit v = Visit.builder()
                .id(300L)
                .patient(patient)
                .doctor(doctor)
                .medicalRecords(new ArrayList<>())
                .prescriptions(new ArrayList<>())
                .build();

        when(visitRepository.findById(300L)).thenReturn(Optional.of(v));

        visitService.delete(300L);

        verify(visitRepository, times(1)).delete(eq(v));

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService, times(1)).log(
                eq("DELETE"),
                eq("Visit"),
                eq(300L),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertTrue(details.contains("patientId=10"));
        assertTrue(details.contains("doctorId=20"));
    }

    @Test
    void delete_shouldThrowNotFound_whenMissing() {
        when(visitRepository.findById(404L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> visitService.delete(404L));
        assertTrue(ex.getMessage().contains("Visit not found"));

        verify(visitRepository, never()).delete(any());
        verify(auditLogService, never()).log(any(), any(), any(), any());
    }
}
