package com.hospital.automation.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AppointmentSeleniumIT {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String baseUrl = System.getenv().getOrDefault("BASE_URL", "http://localhost:9060");

    @BeforeEach
    void setup() throws Exception {
        ChromeOptions options = new ChromeOptions();

        String headless = System.getenv("CHROME_HEADLESS");
        if (headless == null) headless = System.getProperty("chrome.headless", "true");
        if ("true".equalsIgnoreCase(headless)) {
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
        }

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1200,800");

        String remoteUrl = System.getenv("SELENIUM_REMOTE_URL");
        if (remoteUrl != null && !remoteUrl.isBlank()) {
            driver = new RemoteWebDriver(new URL(remoteUrl), options);
        } else {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    @Test
    void createAppointment_viaUi_showsInList() throws IOException {
        driver.get(baseUrl + "/ui/appointments/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        By patientSelBy = By.cssSelector("form select[name='patientId']");
        By doctorSelBy = By.cssSelector("form select[name='doctorId']");

        waitUntilSelectHasRealOptions(patientSelBy);
        waitUntilSelectHasRealOptions(doctorSelBy);

        Select patientSelect = new Select(driver.findElement(patientSelBy));
        boolean patientChosen = chooseFirstRealOptionByValue(patientSelect);
        assertThat(patientChosen).isTrue();

        Select doctorSelect = new Select(driver.findElement(doctorSelBy));
        boolean doctorChosen = chooseFirstRealOptionByValue(doctorSelect);
        assertThat(doctorChosen).isTrue();

        String selectedPatientText = safeText(patientSelect.getFirstSelectedOption());
        String selectedDoctorText = safeText(doctorSelect.getFirstSelectedOption());

        // Department optional
        List<WebElement> deptSelectElems = driver.findElements(By.cssSelector("form select[name='departmentId']"));
        if (!deptSelectElems.isEmpty()) {
            Select deptSelect = new Select(deptSelectElems.get(0));
            chooseFirstRealOptionByValue(deptSelect);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime start = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        setDateTimeLocal("startTime", start.format(fmt));
        setDateTimeLocal("endTime", end.format(fmt));

        List<WebElement> noteInputs = driver.findElements(By.cssSelector("form input[name='note']"));
        if (!noteInputs.isEmpty()) {
            noteInputs.get(0).clear();
            noteInputs.get(0).sendKeys("Selenium test note");
        }

        WebElement submitBtn = findSubmitButton();
        assertThat(submitBtn).isNotNull();
        wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
        submitBtn.click();

        // List sayfası bekle
        wait.until(d -> {
            String u = d.getCurrentUrl();
            return u.equals(baseUrl + "/ui/appointments") || u.equals(baseUrl + "/ui/appointments/");
        });
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        String bodyText = driver.findElement(By.tagName("body")).getText();

        boolean ok =
                (selectedPatientText != null && !selectedPatientText.isBlank() && bodyText.contains(selectedPatientText))
                        || (selectedDoctorText != null && !selectedDoctorText.isBlank() && bodyText.contains(selectedDoctorText))
                        || bodyText.contains("Selenium test note");

        if (!ok) {
            takeScreenshot("appointment-create-missing");
            throw new AssertionError(
                    "Created appointment not found in list page.\n" +
                            "SelectedPatient='" + selectedPatientText + "' SelectedDoctor='" + selectedDoctorText + "'\n" +
                            "Body snippet: " + bodyText.substring(0, Math.min(500, bodyText.length()))
            );
        }

        assertThat(ok).isTrue();
    }

    private void waitUntilSelectHasRealOptions(By selector) {
        wait.until((ExpectedCondition<Boolean>) d -> {
            try {
                WebElement selectEl = d.findElement(selector);
                List<WebElement> opts = selectEl.findElements(By.tagName("option"));
                for (WebElement o : opts) {
                    String v = o.getAttribute("value");
                    if (v != null && !v.isBlank()) return true;
                }
                return false;
            } catch (NoSuchElementException ex) {
                return false;
            }
        });
    }

    private boolean chooseFirstRealOptionByValue(Select select) {
        for (WebElement opt : select.getOptions()) {
            String v = opt.getAttribute("value");
            if (v != null && !v.isBlank()) {
                select.selectByValue(v);
                return true;
            }
        }
        return false;
    }

    private void setDateTimeLocal(String name, String value) {
        WebElement input = driver.findElement(By.cssSelector("form input[type='datetime-local'][name='" + name + "']"));
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "arguments[0].value = arguments[1];" +
                            "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    input, value
            );
        } else {
            input.clear();
            input.sendKeys(value);
        }
    }

    private WebElement findSubmitButton() {
        List<WebElement> buttons = driver.findElements(By.cssSelector("form button[type='submit'], form button.btn-primary"));
        for (WebElement b : buttons) {
            String text = b.getText();
            if (text != null && (text.contains("Create") || text.contains("Update") || text.contains("Save")
                    || text.contains("Kaydet") || text.contains("Oluştur"))) {
                return b;
            }
        }
        return buttons.isEmpty() ? null : buttons.get(0);
    }

    private String safeText(WebElement el) {
        try { return el == null ? null : el.getText(); }
        catch (Exception e) { return null; }
    }

    private void takeScreenshot(String name) throws IOException {
        if (!(driver instanceof TakesScreenshot)) return;

        File scr = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        File target = new File(System.getProperty("java.io.tmpdir"), name + "_" + ts + ".png");

        Files.copy(scr.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Saved screenshot to: " + target.getAbsolutePath());
    }
}



