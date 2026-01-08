package com.hospital.automation.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DoctorSeleniumIT {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String baseUrl = "http://localhost:9060";

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();

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

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    @Test
    void createDoctor_viaUi_showsInList() throws IOException {
        String suffix = shortId();
        String firstName = "Selenium" + suffix;
        String lastName = "Doctor" + suffix;

        driver.get(baseUrl + "/ui/doctors/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        WebElement fn = driver.findElement(By.cssSelector("form input[name='firstName']"));
        WebElement ln = driver.findElement(By.cssSelector("form input[name='lastName']"));
        fn.clear(); fn.sendKeys(firstName);
        ln.clear(); ln.sendKeys(lastName);

        List<WebElement> spec = driver.findElements(By.cssSelector("form input[name='specialization']"));
        if (!spec.isEmpty()) {
            spec.get(0).clear();
            spec.get(0).sendKeys("Cardiology");
        }

        // department select varsa ilk dolu option seç
        List<WebElement> deptSel = driver.findElements(By.cssSelector("form select[name='departmentId']"));
        if (!deptSel.isEmpty()) {
            Select select = new Select(deptSel.get(0));
            chooseFirstRealOptionByValue(select);
        }

        WebElement submitBtn = findSubmitButton();
        assertThat(submitBtn).isNotNull();
        wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
        submitBtn.click();

        wait.until(ExpectedConditions.urlContains("/ui/doctors"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        String body = driver.findElement(By.tagName("body")).getText();
        boolean ok = body.contains(firstName) || body.contains(lastName);

        if (!ok) {
            takeScreenshot("doctor-create-missing");
            throw new AssertionError("Created doctor not found in list page.\nExpectedFirst='" + firstName + "' ExpectedLast='" + lastName +
                    "'\nBody snippet: " + body.substring(0, Math.min(400, body.length())));
        }

        assertThat(ok).isTrue();
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

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 6);
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
