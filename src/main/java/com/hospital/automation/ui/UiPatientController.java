package com.hospital.automation.ui;

import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.PatientRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ui/patients")
public class UiPatientController {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository; // ✅ eklendi

    @GetMapping
    public String list(Model model) {
        model.addAttribute("patients", patientRepository.findAll());
        return "ui/patients/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new PatientForm());
        model.addAttribute("isEdit", false);
        return "ui/patients/form";
    }

    @PostMapping
    public String create(@ModelAttribute("form") PatientForm form,
                         RedirectAttributes ra) {

        Patient patient = Patient.builder()
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .nationalId(form.getNationalId())
                .birthDate(form.getBirthDate())
                .phone(form.getPhone())
                .address(form.getAddress())
                .build();

        patientRepository.save(patient);
        ra.addFlashAttribute("successMessage", "Patient created.");
        return "redirect:/ui/patients";
    }

    // ✅ EDIT FORM
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + id));

        PatientForm form = new PatientForm();
        form.setId(p.getId());
        form.setFirstName(p.getFirstName());
        form.setLastName(p.getLastName());
        form.setNationalId(p.getNationalId());
        form.setBirthDate(p.getBirthDate());
        form.setPhone(p.getPhone());
        form.setAddress(p.getAddress());

        model.addAttribute("form", form);
        model.addAttribute("isEdit", true);
        return "ui/patients/form";
    }

    // ✅ UPDATE
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("form") PatientForm form,
                         RedirectAttributes ra) {

        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + id));

        p.setFirstName(form.getFirstName());
        p.setLastName(form.getLastName());
        p.setNationalId(form.getNationalId());
        p.setBirthDate(form.getBirthDate());
        p.setPhone(form.getPhone());
        p.setAddress(form.getAddress());

        patientRepository.save(p);
        ra.addFlashAttribute("successMessage", "Patient updated.");
        return "redirect:/ui/patients";
    }

    // ✅ DELETE (randevu varsa engelle)
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {

        if (appointmentRepository.existsByPatient_Id(id)) {
            ra.addFlashAttribute("errorMessage",
                    "This patient cannot be deleted because there are appointments linked to them.");
            return "redirect:/ui/patients";
        }

        patientRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Patient deleted.");
        return "redirect:/ui/patients";
    }

    @Data
    public static class PatientForm {
        private Long id; // ✅ edit için gerekli
        private String firstName;
        private String lastName;
        private String nationalId;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate birthDate;

        private String phone;
        private String address;
    }
}
