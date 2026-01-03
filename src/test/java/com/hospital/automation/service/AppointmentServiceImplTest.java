package com.hospital.automation.service;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.AppointmentCreateRequest;
import com.hospital.automation.domain.dto.request.AppointmentUpdateRequest;
import com.hospital.automation.domain.entity.Appointment;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.domain.enums.AppointmentStatus;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import com.hospital.automation.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientRepository patientRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock AuditLogService auditLogService;

    AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                patientRepository,
                doctorRepository,
                departmentRepository,
                auditLogService
        );
    }

    // ------------------------------------------------------------
    // CREATE TESTS
    // ------------------------------------------------------------

    @Test
    void create_shouldThrowBadRequest_whenTimeRangeInvalid() {
        Long patientId = 1L;
        Long doctorId = 10L;

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start; // geçersiz

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                patientId, doctorId, null, start, end, "note"
        );

        assertThrows(BadRequestException.class, () -> appointmentService.create(request));

        verifyNoInteractions(patientRepository, doctorRepository, departmentRepository, appointmentRepository, auditLogService);
    }

    @Test
    void create_shouldThrowNotFound_whenPatientNotExists() {
        Long patientId = 1L;
        Long doctorId = 10L;

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                patientId, doctorId, null, start, end, "note"
        );

        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> appointmentService.create(request));

        verify(patientRepository).findById(patientId);
        verifyNoInteractions(doctorRepository, departmentRepository, appointmentRepository, auditLogService);
    }

    @Test
    void create_shouldThrowNotFound_whenDoctorNotExists() {
        Long patientId = 1L;
        Long doctorId = 10L;

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                patientId, doctorId, null, start, end, "note"
        );

        Patient patient = mock(Patient.class);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> appointmentService.create(request));

        verify(patientRepository).findById(patientId);
        verify(doctorRepository).findById(doctorId);
        verifyNoInteractions(departmentRepository, appointmentRepository, auditLogService);
    }

    @Test
    void create_shouldThrowBadRequest_whenOverlapExists() {
        // FIX: Gereksiz stub kaldırıldı (patient.getId() gibi)
        Long patientId = 1L;
        Long doctorId = 10L;

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                patientId, doctorId, null, start, end, "note"
        );

        Patient patient = mock(Patient.class);

        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(doctorId);

        Department dept = mock(Department.class);
        when(doctor.getDepartment()).thenReturn(dept);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        when(appointmentRepository.existsOverlappingAppointment(
                doctorId, AppointmentStatus.SCHEDULED, start, end
        )).thenReturn(true);

        assertThrows(BadRequestException.class, () -> appointmentService.create(request));

        verify(appointmentRepository).existsOverlappingAppointment(
                doctorId, AppointmentStatus.SCHEDULED, start, end
        );

        verify(appointmentRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(departmentRepository);
    }

    @Test
    void create_shouldPersistAndLog_whenValid_andDepartmentIdNullUsesDoctorsDept() {
        Long patientId = 1L;
        Long doctorId = 10L;

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                patientId, doctorId, null, start, end, "note"
        );

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(patient.getFirstName()).thenReturn("Ali");
        when(patient.getLastName()).thenReturn("Veli");

        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.getFirstName()).thenReturn("Ayşe");
        when(doctor.getLastName()).thenReturn("Demir");
        when(doctor.getSpecialization()).thenReturn("Cardiology");

        Department dept = mock(Department.class);
        when(dept.getId()).thenReturn(100L);
        when(dept.getName()).thenReturn("Cardiology Dept");
        when(doctor.getDepartment()).thenReturn(dept);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        when(appointmentRepository.existsOverlappingAppointment(
                doctorId, AppointmentStatus.SCHEDULED, start, end
        )).thenReturn(false);

        Appointment saved = mock(Appointment.class);
        when(saved.getId()).thenReturn(555L);
        when(saved.getPatient()).thenReturn(patient);
        when(saved.getDoctor()).thenReturn(doctor);
        when(saved.getDepartment()).thenReturn(dept);
        when(saved.getStartTime()).thenReturn(start);
        when(saved.getEndTime()).thenReturn(end);
        when(saved.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(saved.getNote()).thenReturn("note");

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        var response = appointmentService.create(request);

        assertNotNull(response);
        assertEquals(555L, response.id());

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());

        verify(auditLogService).log(
                eq("CREATE"),
                eq("Appointment"),
                eq(555L),
                contains("patientId=" + patientId)
        );

        verifyNoInteractions(departmentRepository);
    }

    @Test
    void create_shouldUseDepartmentRepository_whenDepartmentIdProvided() {
        Long patientId = 1L;
        Long doctorId = 10L;
        Long departmentId = 999L;

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                patientId, doctorId, departmentId, start, end, "note"
        );

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(patient.getFirstName()).thenReturn("Ali");
        when(patient.getLastName()).thenReturn("Veli");

        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.getFirstName()).thenReturn("Ayşe");
        when(doctor.getLastName()).thenReturn("Demir");
        when(doctor.getSpecialization()).thenReturn("Cardiology");

        Department dept = mock(Department.class);
        when(dept.getId()).thenReturn(departmentId);
        when(dept.getName()).thenReturn("Some Dept");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        when(appointmentRepository.existsOverlappingAppointment(
                doctorId, AppointmentStatus.SCHEDULED, start, end
        )).thenReturn(false);

        Appointment saved = mock(Appointment.class);
        when(saved.getId()).thenReturn(777L);
        when(saved.getPatient()).thenReturn(patient);
        when(saved.getDoctor()).thenReturn(doctor);
        when(saved.getDepartment()).thenReturn(dept);
        when(saved.getStartTime()).thenReturn(start);
        when(saved.getEndTime()).thenReturn(end);
        when(saved.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(saved.getNote()).thenReturn("note");
        when(appointmentRepository.save(any())).thenReturn(saved);

        var response = appointmentService.create(request);

        assertEquals(777L, response.id());

        verify(departmentRepository).findById(departmentId);
        verify(auditLogService).log(eq("CREATE"), eq("Appointment"), eq(777L), anyString());
    }

    // ------------------------------------------------------------
    // GET TESTS
    // ------------------------------------------------------------

    @Test
    void getById_shouldThrowNotFound_whenAppointmentNotExists() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> appointmentService.getById(1L));
    }

    @Test
    void getAll_shouldReturnList() {
        Appointment a1 = mock(Appointment.class);
        Appointment a2 = mock(Appointment.class);

        Patient p = mock(Patient.class);
        when(p.getId()).thenReturn(1L);
        when(p.getFirstName()).thenReturn("Ali");
        when(p.getLastName()).thenReturn("Veli");

        Doctor d = mock(Doctor.class);
        when(d.getId()).thenReturn(10L);
        when(d.getFirstName()).thenReturn("Ayşe");
        when(d.getLastName()).thenReturn("Demir");
        when(d.getSpecialization()).thenReturn("Cardiology");

        when(a1.getId()).thenReturn(100L);
        when(a1.getPatient()).thenReturn(p);
        when(a1.getDoctor()).thenReturn(d);
        when(a1.getDepartment()).thenReturn(null);
        when(a1.getStartTime()).thenReturn(LocalDateTime.now().plusDays(1));
        when(a1.getEndTime()).thenReturn(LocalDateTime.now().plusDays(1).plusMinutes(30));
        when(a1.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(a1.getNote()).thenReturn(null);

        when(a2.getId()).thenReturn(101L);
        when(a2.getPatient()).thenReturn(p);
        when(a2.getDoctor()).thenReturn(d);
        when(a2.getDepartment()).thenReturn(null);
        when(a2.getStartTime()).thenReturn(LocalDateTime.now().plusDays(2));
        when(a2.getEndTime()).thenReturn(LocalDateTime.now().plusDays(2).plusMinutes(30));
        when(a2.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(a2.getNote()).thenReturn("x");

        when(appointmentRepository.findAll()).thenReturn(List.of(a1, a2));

        var list = appointmentService.getAll();

        assertEquals(2, list.size());
        assertEquals(100L, list.get(0).id());
        assertEquals(101L, list.get(1).id());
    }

    // ------------------------------------------------------------
    // UPDATE TESTS
    // ------------------------------------------------------------

    @Test
    void update_shouldThrowNotFound_whenAppointmentNotExists() {
        // FIX: Gereksiz stub kaldırıldı. Appointment bulunamadığı için validateTimeRange'den sonra direkt NotFound fırlatacak.
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        AppointmentUpdateRequest request = mock(AppointmentUpdateRequest.class);
        when(request.startTime()).thenReturn(LocalDateTime.now().plusDays(1));
        when(request.endTime()).thenReturn(LocalDateTime.now().plusDays(1).plusMinutes(30));

        assertThrows(NotFoundException.class, () -> appointmentService.update(1L, request));
    }

    @Test
    void update_shouldThrowBadRequest_whenOverlap_andStatusScheduled() {
        Long apptId = 50L;
        Long doctorId = 10L;

        AppointmentUpdateRequest request = mock(AppointmentUpdateRequest.class);
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        when(request.startTime()).thenReturn(start);
        when(request.endTime()).thenReturn(end);
        when(request.status()).thenReturn(AppointmentStatus.SCHEDULED);

        Appointment a = mock(Appointment.class);
        when(a.getId()).thenReturn(apptId); // ✅ FIX: excludeId artık 50L olacak

        Doctor d = mock(Doctor.class);
        when(d.getId()).thenReturn(doctorId);
        when(a.getDoctor()).thenReturn(d);

        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(a));

        when(appointmentRepository.existsOverlappingAppointmentExcludingId(
                doctorId, apptId, AppointmentStatus.SCHEDULED, start, end
        )).thenReturn(true);

        assertThrows(BadRequestException.class, () -> appointmentService.update(apptId, request));

        verify(a, never()).setStartTime(any());
        verify(a, never()).setEndTime(any());
        verify(a, never()).setStatus(any());
        verify(a, never()).setNote(any());
        verifyNoInteractions(auditLogService);
    }


    @Test
    void update_shouldSetFieldsAndLog_whenValid() {
        Long apptId = 50L;
        Long doctorId = 10L;

        AppointmentUpdateRequest request = mock(AppointmentUpdateRequest.class);
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        when(request.startTime()).thenReturn(start);
        when(request.endTime()).thenReturn(end);
        when(request.status()).thenReturn(AppointmentStatus.SCHEDULED);
        when(request.note()).thenReturn("updated");

        Appointment a = mock(Appointment.class);

        Patient p = mock(Patient.class);
        when(p.getId()).thenReturn(1L);
        when(p.getFirstName()).thenReturn("Ali");
        when(p.getLastName()).thenReturn("Veli");

        Doctor d = mock(Doctor.class);
        when(d.getId()).thenReturn(doctorId);
        when(d.getFirstName()).thenReturn("Ayşe");
        when(d.getLastName()).thenReturn("Demir");
        when(d.getSpecialization()).thenReturn("Cardiology");

        when(a.getPatient()).thenReturn(p);
        when(a.getDoctor()).thenReturn(d);
        when(a.getDepartment()).thenReturn(null);

        // toResponse için getter'lar
        when(a.getStartTime()).thenReturn(start);
        when(a.getEndTime()).thenReturn(end);
        when(a.getStatus()).thenReturn(AppointmentStatus.SCHEDULED);
        when(a.getNote()).thenReturn("updated");
        when(a.getId()).thenReturn(apptId);

        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(a));
        when(appointmentRepository.existsOverlappingAppointmentExcludingId(
                doctorId, apptId, AppointmentStatus.SCHEDULED, start, end
        )).thenReturn(false);

        var response = appointmentService.update(apptId, request);

        assertEquals(apptId, response.id());

        verify(a).setStartTime(start);
        verify(a).setEndTime(end);
        verify(a).setStatus(AppointmentStatus.SCHEDULED);
        verify(a).setNote("updated");

        verify(auditLogService).log(
                eq("UPDATE"),
                eq("Appointment"),
                eq(apptId),
                contains("status=")
        );
    }

    // ------------------------------------------------------------
    // DELETE TESTS
    // ------------------------------------------------------------

    @Test
    void delete_shouldThrowNotFound_whenAppointmentNotExists() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> appointmentService.delete(1L));
    }

    @Test
    void delete_shouldDeleteAndLog_whenExists() {
        // FIX: Gereksiz stub kaldırıldı (a.getId vb. gerekmiyordu)
        Long apptId = 1L;

        Appointment a = mock(Appointment.class);

        Patient p = mock(Patient.class);
        when(p.getId()).thenReturn(100L);

        Doctor d = mock(Doctor.class);
        when(d.getId()).thenReturn(200L);

        when(a.getPatient()).thenReturn(p);
        when(a.getDoctor()).thenReturn(d);

        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(a));

        appointmentService.delete(apptId);

        verify(appointmentRepository).delete(a);
        verify(auditLogService).log(
                eq("DELETE"),
                eq("Appointment"),
                eq(apptId),
                contains("doctorId=")
        );
    }
}
