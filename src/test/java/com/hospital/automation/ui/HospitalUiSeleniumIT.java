package com.hospital.automation.ui;

import com.hospital.automation.domain.entity.Department;
import com.hospital.automation.domain.entity.Doctor;
import com.hospital.automation.domain.entity.Patient;
import com.hospital.automation.domain.enums.AppointmentStatus;
import com.hospital.automation.repository.DepartmentRepository;
import com.hospital.automation.repository.DoctorRepository;
import com.hospital.automation.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class HospitalUiSeleniumIT extends SeleniumBaseIT {

    @Autowired DepartmentRepository departmentRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired PatientRepository patientRepository;

    private static final DateTimeFormatter DT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    /**
     * SENARYO 1:
     * Department CRUD (Create -> Edit -> Delete)
     */
    @Test
    void scenario1_department_crud() {
        // LIST
        open("/ui/departments");

        // CREATE
        driver.findElement(By.cssSelector("[data-testid='dept-new']")).click();
        driver.findElement(By.cssSelector("[data-testid='dept-name']")).sendKeys("Cardiology");
        driver.findElement(By.cssSelector("[data-testid='dept-create']")).click();

        assertNotNull(findRowContaining("Cardiology"));

        // EDIT
        WebElement row = findRowContaining("Cardiology");
        assertNotNull(row);
        row.findElement(By.linkText("Edit")).click();

        WebElement nameInput = driver.findElement(By.name("name"));
        nameInput.clear();
        nameInput.sendKeys("Cardiology-Updated");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        assertNotNull(findRowContaining("Cardiology-Updated"));

        // DELETE
        row = findRowContaining("Cardiology-Updated");
        assertNotNull(row);
        row.findElement(By.cssSelector("form button.btn-danger")).click();
        acceptConfirmIfPresent();

        assertNull(findRowContaining("Cardiology-Updated"));
    }

    /**
     * SENARYO 2:
     * Patient CRUD (Create -> Edit -> Delete)
     */
    @Test
    void scenario2_patient_crud() {
        open("/ui/patients");

        // CREATE
        driver.findElement(By.cssSelector("[data-testid='patient-new']")).click();
        driver.findElement(By.cssSelector("[data-testid='pat-firstName']")).sendKeys("Ayse");
        driver.findElement(By.cssSelector("[data-testid='pat-lastName']")).sendKeys("Kaya");
        driver.findElement(By.cssSelector("[data-testid='pat-nationalId']")).sendKeys("11111111111");
        driver.findElement(By.cssSelector("[data-testid='pat-birthDate']")).sendKeys(LocalDate.of(2000, 1, 1).toString());
        driver.findElement(By.cssSelector("[data-testid='pat-phone']")).sendKeys("05550000000");
        driver.findElement(By.cssSelector("[data-testid='pat-address']")).sendKeys("Istanbul");
        driver.findElement(By.cssSelector("[data-testid='pat-create']")).click();

        assertNotNull(findRowContaining("Ayse Kaya"));

        // EDIT
        WebElement row = findRowContaining("Ayse Kaya");
        assertNotNull(row);
        row.findElement(By.linkText("Edit")).click();

        WebElement phone = driver.findElement(By.id("phone"));
        phone.clear();
        phone.sendKeys("05551112233");

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebElement updatedRow = findRowContaining("Ayse Kaya");
        assertNotNull(updatedRow);
        assertTrue(updatedRow.getText().contains("05551112233"));

        // DELETE
        row = findRowContaining("Ayse Kaya");
        assertNotNull(row);
        row.findElement(By.cssSelector("form button.btn-danger")).click();
        acceptConfirmIfPresent();

        assertNull(findRowContaining("Ayse Kaya"));
    }

    /**
     * SENARYO 3 (DÜZELTİLDİ):
     * Appointment Create + Delete
     *
     * Not: UI list sayfası NOTE göstermiyor, o yüzden NOTE'a göre assert yok.
     * Bunun yerine: Patient + Doctor + Department + StartDate'e göre row buluyoruz.
     */
    @Test
    void scenario3_appointment_create_and_delete() {

        // --- Seed data (repo ile) ---
        Department dep = departmentRepository.save(Department.builder().name("Neurology").build());

        Doctor doc = doctorRepository.save(Doctor.builder()
                .firstName("Mehmet")
                .lastName("Demir")
                .specialization("Neurologist")
                .department(dep)
                .build());

        Patient pat = patientRepository.save(Patient.builder()
                .firstName("Zeynep")
                .lastName("Arslan")
                .nationalId("22222222222")
                .birthDate(LocalDate.of(1999, 5, 10))
                .phone("05001112233")
                .address("Kadikoy")
                .build());

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String patientFull = pat.getFirstName() + " " + pat.getLastName();
        String doctorFull = doc.getFirstName() + " " + doc.getLastName();
        String depName = dep.getName();

        // --- UI: appointments list ---
        open("/ui/appointments");
        waitForPageReady(wait);
        failIfRedirectedToLogin();

        // New Appointment butonu: href ile garanti
        WebElement newBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a[href$='/ui/appointments/new'], a[href*='/ui/appointments/new']")
        ));
        newBtn.click();

        wait.until(d -> d.getCurrentUrl().contains("/ui/appointments/new"));
        waitForPageReady(wait);

        failIfRedirectedToLogin();
        failIfErrorPage();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("form")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("patientId")));


        // --- Form geldi mi? ---
        waitForPageReady(wait);
        failIfRedirectedToLogin();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("patientId")));
        new Select(driver.findElement(By.name("patientId"))).selectByValue(pat.getId().toString());
        new Select(driver.findElement(By.name("doctorId"))).selectByValue(doc.getId().toString());
        new Select(driver.findElement(By.name("departmentId"))).selectByValue(dep.getId().toString());

        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        setDateTimeLocal(driver.findElement(By.name("startTime")), start.format(DT_LOCAL));
        setDateTimeLocal(driver.findElement(By.name("endTime")), end.format(DT_LOCAL));

        new Select(driver.findElement(By.name("status")))
                .selectByValue(AppointmentStatus.SCHEDULED.name());

        // NOTE alanı varsa doldurabilirsin ama listede görünmüyor
        String note = "UI Selenium appointment " + System.currentTimeMillis();
        if (!driver.findElements(By.name("note")).isEmpty()) {
            driver.findElement(By.name("note")).sendKeys(note);
        }

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // --- Submit sonrası listeye dönüş ---
        waitForPageReady(wait);
        failIfRedirectedToLogin();

        // Artık NOTE beklemek YOK. Row'u Patient+Doctor+Department+StartDate ile bul.
        String startDatePart = start.toLocalDate().toString(); // ör: 2026-01-08
        WebElement row = wait.until(d -> findAppointmentRow(patientFull, doctorFull, depName, startDatePart));
        assertNotNull(row, "Yeni appointment row'u bulunamadı!");

        // Güçlü assert: row text içinde isimler var mı?
        String rowText = row.getText();
        assertTrue(rowText.contains("Zeynep"), "Row'da patient adı yok");
        assertTrue(rowText.contains("Mehmet"), "Row'da doctor adı yok");
        assertTrue(rowText.contains("Neurology"), "Row'da department yok");

        // --- Delete: row içindeki delete butonu ---
        WebElement deleteBtn = row.findElement(By.cssSelector("form button.btn-danger"));
        deleteBtn.click();
        acceptConfirmIfPresent();

        // Silinmeyi bekle: aynı row artık bulunmamalı
        waitForPageReady(wait);
        wait.until(d -> findAppointmentRow(patientFull, doctorFull, depName, startDatePart) == null);
    }

    // ---------------- helpers ----------------

    private void waitForPageReady(WebDriverWait wait) {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState")));
    }

    private void failIfRedirectedToLogin() {
        String url = driver.getCurrentUrl();

        boolean looksLikeLoginUrl = url.contains("/login") || url.contains("/signin");
        boolean looksLikeLoginForm =
                !driver.findElements(By.cssSelector("input[type='password']")).isEmpty() &&
                        (!driver.findElements(By.name("username")).isEmpty()
                                || !driver.findElements(By.id("username")).isEmpty()
                                || !driver.findElements(By.cssSelector("input[type='email']")).isEmpty());

        if (looksLikeLoginUrl || looksLikeLoginForm) {
            fail("UI sayfası yerine LOGIN sayfasına yönlendirilmişsin.\n" +
                    "URL: " + url + "\n" +
                    "Çözüm: test profilinde /ui/** için permitAll yap veya testte otomatik login ekle.");
        }
    }

    /**
     * Appointments tablosunda satır bulma:
     * - patientFull (ör: "Zeynep Arslan")
     * - doctorFull  (ör: "Mehmet Demir")
     * - depName     (ör: "Neurology")
     * - startDatePart (ör: "2026-01-08") -> startTime formatı değişse bile date kısmı genelde içerir
     */
    private WebElement findAppointmentRow(String patientFull, String doctorFull, String depName, String startDatePart) {
        // Tablo row: içinde patient + doctor + department + startDatePart geçen satırı bul
        String xp =
                "//table//tbody//tr[" +
                        "contains(normalize-space(.),'" + escapeXPath(patientFull) + "') and " +
                        "contains(normalize-space(.),'" + escapeXPath(doctorFull) + "') and " +
                        "contains(normalize-space(.),'" + escapeXPath(depName) + "') and " +
                        "contains(normalize-space(.),'" + escapeXPath(startDatePart) + "')" +
                        "]";

        var rows = driver.findElements(By.xpath(xp));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String escapeXPath(String s) {
        // Basit kaçış: tek tırnak yoksa direkt kullanırız.
        // (Bizim isimlerde tek tırnak beklemiyoruz; istersen concat'lı gelişmiş sürümü de eklerim.)
        return s.replace("'", "");
    }

    private void setDateTimeLocal(WebElement input, String value) {
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.BACK_SPACE);
        input.sendKeys(value);
    }

    private void failIfErrorPage() {
        String title = driver.getTitle();
        String src = driver.getPageSource();

        if (title != null && title.toLowerCase().contains("error")) {
            fail("Error page açıldı. TITLE=" + title + " | URL=" + driver.getCurrentUrl());
        }
        if (src.contains("Whitelabel Error Page") || src.contains("status=403") || src.contains("status=401")) {
            fail("Whitelabel/401/403 error page açıldı.\nURL=" + driver.getCurrentUrl());
        }
    }

}
