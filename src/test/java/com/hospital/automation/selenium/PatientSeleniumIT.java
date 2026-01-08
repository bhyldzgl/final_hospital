package com.hospital.automation.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebDriverBuilder;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class PatientSeleniumIT {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String baseUrl = "http://localhost:9060";

    @BeforeEach
    void setup() throws MalformedURLException {
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
            // Use RemoteWebDriver (Selenium Grid / standalone container)
            driver = new RemoteWebDriver(new URL(remoteUrl), options);
        } else {
            // Local ChromeDriver via WebDriverManager
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
        // Navigate to new patient form
        driver.get(baseUrl + "/ui/patients/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        // Fill basic fields
        driver.findElement(By.cssSelector("form input[name='firstName']")).sendKeys("Selenium");
        driver.findElement(By.cssSelector("form input[name='lastName']")).sendKeys("Patient");

        // nationalId (if present)
        List<WebElement> nat = driver.findElements(By.cssSelector("form input[name='nationalId']"));
        if (!nat.isEmpty()) nat.get(0).sendKeys("12345678901");

        // birthDate - use yyyy-MM-dd
        List<WebElement> birth = driver.findElements(By.cssSelector("form input[name='birthDate']"));
        if (!birth.isEmpty()) {
            String dateStr = LocalDate.now().minusYears(30).format(DateTimeFormatter.ISO_DATE);
            birth.get(0).clear();
            // Use JS to set value to avoid localization issues with sendKeys on date inputs
            if (driver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", birth.get(0), dateStr);
            } else {
                birth.get(0).sendKeys(dateStr);
            }
        }

        // phone/address
        List<WebElement> phone = driver.findElements(By.cssSelector("form input[name='phone']"));
        if (!phone.isEmpty()) phone.get(0).sendKeys("+905551112233");
        List<WebElement> addr = driver.findElements(By.cssSelector("form textarea[name='address'], form input[name='address']"));
        if (!addr.isEmpty()) addr.get(0).sendKeys("Test address");

        // Submit
        WebElement submitBtn = findSubmitButton();
        assertThat(submitBtn).isNotNull();
        submitBtn.click();

        // After submit expect redirect to /ui/patients
        wait.until(ExpectedConditions.urlContains("/ui/patients"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        String body = driver.findElement(By.tagName("body")).getText();
        if (body.contains("Selenium Patient") || body.contains("Selenium") ) {
            assertThat(body).contains("Selenium");
        } else {
            takeScreenshot("patient-create-missing");
            throw new AssertionError("Created patient not found in list page. Body snippet: " + body.substring(0, Math.min(300, body.length())));
        }
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

    private void takeScreenshot(String name) throws IOException {
        if (!(driver instanceof TakesScreenshot)) return;
        File scr = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File target = new File(System.getProperty("java.io.tmpdir"), name + ".png");
        Files.copy(scr.toPath(), target.toPath());
        System.out.println("Saved screenshot to: " + target.getAbsolutePath());
    }
}
