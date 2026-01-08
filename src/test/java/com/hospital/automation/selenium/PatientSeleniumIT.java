package com.hospital.automation.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class PatientSeleniumIT {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String baseUrl = "http://localhost:9060";

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

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    @Test
    void createPatient_viaUi_showsInList() throws IOException {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String firstName = "Selenium";
        String lastName = "Patient" + suffix;
        String nationalId = random11Digits();

        driver.get(baseUrl + "/ui/patients/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        // basic
        WebElement fn = driver.findElement(By.cssSelector("form input[name='firstName']"));
        WebElement ln = driver.findElement(By.cssSelector("form input[name='lastName']"));
        fn.clear(); fn.sendKeys(firstName);
        ln.clear(); ln.sendKeys(lastName);

        // nationalId (if present)
        List<WebElement> nat = driver.findElements(By.cssSelector("form input[name='nationalId']"));
        if (!nat.isEmpty()) {
            nat.get(0).clear();
            nat.get(0).sendKeys(nationalId);
        }

        // birthDate yyyy-MM-dd
        List<WebElement> birth = driver.findElements(By.cssSelector("form input[name='birthDate']"));
        if (!birth.isEmpty()) {
            String dateStr = LocalDate.now().minusYears(30).format(DateTimeFormatter.ISO_DATE);
            WebElement birthEl = birth.get(0);
            if (driver instanceof JavascriptExecutor js) {
                js.executeScript(
                        "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                        birthEl, dateStr
                );
            } else {
                birthEl.clear();
                birthEl.sendKeys(dateStr);
            }
        }

        // phone/address
        List<WebElement> phone = driver.findElements(By.cssSelector("form input[name='phone']"));
        if (!phone.isEmpty()) {
            phone.get(0).clear();
            phone.get(0).sendKeys("+90555" + ThreadLocalRandom.current().nextInt(1000000, 9999999));
        }

        List<WebElement> addr = driver.findElements(By.cssSelector("form textarea[name='address'], form input[name='address']"));
        if (!addr.isEmpty()) {
            addr.get(0).clear();
            addr.get(0).sendKeys("Test address " + suffix);
        }

        WebElement submitBtn = findSubmitButton();
        assertThat(submitBtn).isNotNull();
        wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
        submitBtn.click();

        wait.until(ExpectedConditions.urlContains("/ui/patients"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        String body = driver.findElement(By.tagName("body")).getText();

        // En sağlam assert: unique lastName veya nationalId’den biri görünmeli
        boolean ok = body.contains(lastName) || body.contains(nationalId) || body.contains(firstName);

        if (!ok) {
            takeScreenshot("patient-create-missing");
            throw new AssertionError("Created patient not found in list page.\nExpectedLast='" + lastName + "' NationalId='" + nationalId +
                    "'\nBody snippet: " + body.substring(0, Math.min(400, body.length())));
        }

        assertThat(ok).isTrue();
    }

    private WebElement findSubmitButton() {
        List<WebElement> buttons = driver.findElements(By.cssSelector("form button[type='submit'], form button.btn-primary"));
        for (WebElement b : buttons) {
            String text = b.getText();
            if (text != null && (text.contains("Create") || text.contains("Save") || text.contains("Kaydet") || text.contains("Oluştur"))) {
                return b;
            }
        }
        return buttons.isEmpty() ? null : buttons.get(0);
    }

    private String random11Digits() {
        long n = ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L);
        return Long.toString(n);
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
