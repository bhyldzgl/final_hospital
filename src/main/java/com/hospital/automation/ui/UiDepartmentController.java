package com.hospital.automation.ui;

import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.repository.AppointmentRepository;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ui/departments")
public class UiDepartmentController {

    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        return "ui/departments/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("department", new Department());
        model.addAttribute("isEdit", false);
        return "ui/departments/form";
    }

    @PostMapping
    public String create(@ModelAttribute("department") Department department,
                         RedirectAttributes ra) {
        try {
            departmentRepository.save(department);
            ra.addFlashAttribute("successMessage", "Department created.");
            return "redirect:/ui/departments";
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Department name must be unique.");
            return "redirect:/ui/departments/new";
        }
    }

    // ✅ EDIT FORM
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Department dep = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));

        model.addAttribute("department", dep);
        model.addAttribute("isEdit", true);
        return "ui/departments/form";
    }

    // ✅ UPDATE
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("department") Department form,
                         RedirectAttributes ra) {

        Department dep = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found: " + id));

        dep.setName(form.getName());

        try {
            departmentRepository.save(dep);
            ra.addFlashAttribute("successMessage", "Department updated.");
            return "redirect:/ui/departments";
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Department name must be unique.");
            return "redirect:/ui/departments/" + id + "/edit";
        }
    }

    // ✅ DELETE (doktor/randevu varsa engelle)
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {

        if (doctorRepository.existsByDepartment_Id(id)) {
            ra.addFlashAttribute("errorMessage",
                    "This department cannot be deleted because doctors are linked to it.");
            return "redirect:/ui/departments";
        }

        if (appointmentRepository.existsByDepartment_Id(id)) {
            ra.addFlashAttribute("errorMessage",
                    "This department cannot be deleted because appointments are linked to it.");
            return "redirect:/ui/departments";
        }

        departmentRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Department deleted.");
        return "redirect:/ui/departments";
    }
}
