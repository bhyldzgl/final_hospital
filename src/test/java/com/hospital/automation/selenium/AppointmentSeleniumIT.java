package com.hospital.automation.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AppointmentSeleniumIT {

    private WebDriver driver;
    private WebDriverWait wait;

    // Default URL used by Jenkins/docker-compose in this project
    private final String baseUrl = "http://localhost:9060";

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        // Allow toggling headless via env var CHROME_HEADLESS (true/false)
        String headless = System.getenv("CHROME_HEADLESS");
        if (headless == null) headless = System.getProperty("chrome.headless", "true");
        if ("true".equalsIgnoreCase(headless)) {
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
        }

        // Recommended flags for CI
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1200,800");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void createAppointment_viaUi_showsInList() throws IOException {
        // Navigate to new appointment form
        driver.get(baseUrl + "/ui/appointments/new");

        // Wait for form to be present
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        // Wait for selects to be populated (patients/doctors loaded by controller)
        waitUntilSelectHasOptions(By.cssSelector("form select[name='patientId']"));
        waitUntilSelectHasOptions(By.cssSelector("form select[name='doctorId']"));

        // Select first non-empty patient (use rendered name attribute)
        Select patientSelect = new Select(driver.findElement(By.cssSelector("form select[name='patientId']")));
        boolean patientChosen = chooseFirstNonEmptyOption(patientSelect);
        assertThat(patientChosen).isTrue();

        // Select first non-empty doctor
        Select doctorSelect = new Select(driver.findElement(By.cssSelector("form select[name='doctorId']")));
        boolean doctorChosen = chooseFirstNonEmptyOption(doctorSelect);
        assertThat(doctorChosen).isTrue();

        // Department is optional - try select first option if present
        List<WebElement> deptSelectElems = driver.findElements(By.cssSelector("form select[name='departmentId']"));
        if (!deptSelectElems.isEmpty()) {
            Select deptSelect = new Select(deptSelectElems.get(0));
            chooseFirstNonEmptyOption(deptSelect);
        }

        // Fill start/end times (datetime-local expects yyyy-MM-dd'T'HH:mm)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime start = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        String startStr = start.format(fmt);
        String endStr = end.format(fmt);

        WebElement startInput = driver.findElement(By.cssSelector("form input[type='datetime-local'][name='startTime']"));
        startInput.clear();
        // Use JS to set value to avoid sendKeys issues on datetime-local
        if (driver instanceof JavascriptExecutor) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", startInput, startStr);
        } else {
            startInput.sendKeys(startStr);
        }

        WebElement endInput = driver.findElement(By.cssSelector("form input[type='datetime-local'][name='endTime']"));
        endInput.clear();
        if (driver instanceof JavascriptExecutor) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", endInput, endStr);
        } else {
            endInput.sendKeys(endStr);
        }

        // Optional note
        List<WebElement> noteInputs = driver.findElements(By.cssSelector("form input[name='note']"));
        if (!noteInputs.isEmpty()) {
            noteInputs.get(0).sendKeys("Selenium test note");
        }

        // Submit the form - find submit button by text 'Create' or button[type=submit]
        WebElement submitBtn = null;
        List<WebElement> buttons = driver.findElements(By.cssSelector("form button[type='submit'], form button.btn-primary"));
        for (WebElement b : buttons) {
            String text = b.getText();
            if (text != null && (text.contains("Create") || text.contains("Update") || text.contains("Kaydet"))) {
                submitBtn = b;
                break;
            }
        }
        if (submitBtn == null && !buttons.isEmpty()) submitBtn = buttons.get(0);
        assertThat(submitBtn).isNotNull();

        // Wait until clickable and click
        wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
        submitBtn.click();

        // After submit we should be redirected to /ui/appointments
        wait.until(ExpectedConditions.urlContains("/ui/appointments"));

        // Wait for the list table or appointments content
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        // Basic assertion: page contains our note text or a row with the doctor's/patient's name
        String bodyText = driver.findElement(By.tagName("body")).getText();

        // If note was set, assert it appears; otherwise assert presence of start time
        if (bodyText.contains("Selenium test note")) {
            assertThat(bodyText).contains("Selenium test note");
        } else if (bodyText.contains(start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))) {
            assertThat(bodyText).contains(start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            // Take screenshot to help debugging
            takeScreenshot("appointment-create-missing");
            throw new AssertionError("Created appointment not found in list page. Body snippet: " + bodyText.substring(0, Math.min(300, bodyText.length())));
        }
    }

    private void waitUntilSelectHasOptions(By selector) {
        wait.until((ExpectedCondition<Boolean>) driver -> {
            try {
                WebElement e = driver.findElement(selector);
                List<WebElement> opts = e.findElements(By.tagName("option"));
                for (WebElement o : opts) {
                    String v = o.getAttribute("value");
                    String t = o.getText();
                    if ((v != null && !v.isBlank()) || (t != null && !t.isBlank())) return true;
                }
                return false;
            } catch (NoSuchElementException ex) {
                return false;
            }
        });
    }

    private boolean chooseFirstNonEmptyOption(Select select) {
        for (WebElement opt : select.getOptions()) {
            String v = opt.getAttribute("value");
            String t = opt.getText();
            if (v != null && !v.isBlank()) {
                select.selectByValue(v);
                return true;
            }
            if (t != null && !t.isBlank()) {
                // fallback: select by visible text
                select.selectByVisibleText(t);
                return true;
            }
        }
        return false;
    }

    private void takeScreenshot(String name) throws IOException {
        if (!(driver instanceof TakesScreenshot)) return;
        File scr = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File target = new File(System.getProperty("java.io.tmpdir"), name + ".png");
        Files.copy(scr.toPath(), target.toPath());
        System.out.println("Saved screenshot to: " + target.getAbsolutePath());
    }
}
