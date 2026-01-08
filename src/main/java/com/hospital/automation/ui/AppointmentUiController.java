package com.hospital.automation.ui;

import com.hospital.automation.domain.entity.Appointment;
import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.domain.enums.AppointmentStatus;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Controller
@RequestMapping("/ui/appointments")
@RequiredArgsConstructor
public class AppointmentUiController {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("appointments", appointmentRepository.findAll());
        return "ui/appointments/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("appointmentForm", new AppointmentForm());
        model.addAttribute("patients", patientRepository.findAll());
        model.addAttribute("doctors", doctorRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("statuses", Arrays.asList(AppointmentStatus.values()));
        model.addAttribute("isEdit", false);
        return "ui/appointments/form";
    }

    @PostMapping
    public String create(@ModelAttribute("appointmentForm") AppointmentForm form,
                         RedirectAttributes ra) {

        Patient patient = patientRepository.findById(form.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found: " + form.getPatientId()));

        Doctor doctor = doctorRepository.findById(form.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + form.getDoctorId()));

        Department dep = null;
        if (form.getDepartmentId() != null) {
            dep = departmentRepository.findById(form.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + form.getDepartmentId()));
        }

        Appointment a = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .department(dep)
                .startTime(form.getStartTime())
                .endTime(form.getEndTime())
                .status(form.getStatus() == null ? AppointmentStatus.SCHEDULED : form.getStatus())
                .note(form.getNote())
                .build();

        appointmentRepository.save(a);
        ra.addFlashAttribute("successMessage", "Appointment created.");
        return "redirect:/ui/appointments";
    }

    // ✅ EDIT FORM
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));

        AppointmentForm form = new AppointmentForm();
        form.setId(a.getId());
        form.setPatientId(a.getPatient().getId());
        form.setDoctorId(a.getDoctor().getId());
        if (a.getDepartment() != null) form.setDepartmentId(a.getDepartment().getId());
        form.setStartTime(a.getStartTime());
        form.setEndTime(a.getEndTime());
        form.setStatus(a.getStatus());
        form.setNote(a.getNote());

        model.addAttribute("appointmentForm", form);
        model.addAttribute("patients", patientRepository.findAll());
        model.addAttribute("doctors", doctorRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("statuses", Arrays.asList(AppointmentStatus.values()));
        model.addAttribute("isEdit", true);
        return "ui/appointments/form";
    }

    // ✅ UPDATE
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("appointmentForm") AppointmentForm form,
                         RedirectAttributes ra) {

        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));

        Patient patient = patientRepository.findById(form.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found: " + form.getPatientId()));

        Doctor doctor = doctorRepository.findById(form.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + form.getDoctorId()));

        Department dep = null;
        if (form.getDepartmentId() != null) {
            dep = departmentRepository.findById(form.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + form.getDepartmentId()));
        }

        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setDepartment(dep);
        a.setStartTime(form.getStartTime());
        a.setEndTime(form.getEndTime());
        a.setStatus(form.getStatus() == null ? AppointmentStatus.SCHEDULED : form.getStatus());
        a.setNote(form.getNote());

        appointmentRepository.save(a);
        ra.addFlashAttribute("successMessage", "Appointment updated.");
        return "redirect:/ui/appointments";
    }

    // ✅ DELETE
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        appointmentRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Appointment deleted.");
        return "redirect:/ui/appointments";
    }

    public static class AppointmentForm {
        private Long id;
        private Long patientId;
        private Long doctorId;
        private Long departmentId;

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        private LocalDateTime startTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        private LocalDateTime endTime;

        private AppointmentStatus status = AppointmentStatus.SCHEDULED;
        private String note;

        public AppointmentForm() {
            this.startTime = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
            this.endTime = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getPatientId() { return patientId; }
        public void setPatientId(Long patientId) { this.patientId = patientId; }

        public Long getDoctorId() { return doctorId; }
        public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public AppointmentStatus getStatus() { return status; }
        public void setStatus(AppointmentStatus status) { this.status = status; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
