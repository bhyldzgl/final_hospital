package com.hospital.automation.ui;

import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui/doctors")
@RequiredArgsConstructor
public class DoctorUiController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository; // ✅ eklendi

    // LIST
    @GetMapping
    public String list(Model model) {
        model.addAttribute("doctors", doctorRepository.findAll());
        return "ui/doctors/list";
    }

    // CREATE FORM
    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("doctorForm", new DoctorForm());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("isEdit", false);
        return "ui/doctors/form";
    }

    // CREATE
    @PostMapping
    public String create(@ModelAttribute("doctorForm") DoctorForm form) {

        Department dep = null;
        if (form.getDepartmentId() != null) {
            dep = departmentRepository.findById(form.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + form.getDepartmentId()));
        }

        Doctor d = Doctor.builder()
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .specialization(form.getSpecialization())
                .department(dep)
                .build();

        doctorRepository.save(d);
        return "redirect:/ui/doctors";
    }

    // EDIT FORM
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + id));

        DoctorForm form = new DoctorForm();
        form.setId(doctor.getId());
        form.setFirstName(doctor.getFirstName());
        form.setLastName(doctor.getLastName());
        form.setSpecialization(doctor.getSpecialization());
        if (doctor.getDepartment() != null) {
            form.setDepartmentId(doctor.getDepartment().getId());
        }

        model.addAttribute("doctorForm", form);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("isEdit", true);
        return "ui/doctors/form";
    }

    // UPDATE
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("doctorForm") DoctorForm form) {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + id));

        Department dep = null;
        if (form.getDepartmentId() != null) {
            dep = departmentRepository.findById(form.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + form.getDepartmentId()));
        }

        doctor.setFirstName(form.getFirstName());
        doctor.setLastName(form.getLastName());
        doctor.setSpecialization(form.getSpecialization());
        doctor.setDepartment(dep);

        doctorRepository.save(doctor);
        return "redirect:/ui/doctors";
    }

    // DELETE (POST) - ✅ önce randevu kontrolü
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {

        if (appointmentRepository.existsByDoctor_Id(id)) {
            ra.addFlashAttribute("error",
                    "Bu doktor silinemez çünkü bağlı randevuları var. Önce randevuları silin veya doktoru değiştirin.");
            return "redirect:/ui/doctors";
        }

        doctorRepository.deleteById(id);
        ra.addFlashAttribute("success", "Doktor başarıyla silindi.");
        return "redirect:/ui/doctors";
    }

    // ---- FORM DTO ----
    public static class DoctorForm {
        private Long id;
        private String firstName;
        private String lastName;
        private String specialization;
        private Long departmentId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    }
}
