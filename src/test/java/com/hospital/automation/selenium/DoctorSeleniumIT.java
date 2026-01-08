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
import java.time.Duration;
import java.util.List;

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
        driver.get(baseUrl + "/ui/doctors/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        driver.findElement(By.cssSelector("form input[name='firstName']")).sendKeys("Selenium");
        driver.findElement(By.cssSelector("form input[name='lastName']")).sendKeys("Doctor");
        List<WebElement> spec = driver.findElements(By.cssSelector("form input[name='specialization']"));
        if (!spec.isEmpty()) spec.get(0).sendKeys("Cardiology");

        // choose department if select present
        List<WebElement> deptSel = driver.findElements(By.cssSelector("form select[name='departmentId']"));
        if (!deptSel.isEmpty()) {
            Select select = new Select(deptSel.get(0));
            chooseFirstNonEmptyOption(select);
        }

        WebElement submitBtn = findSubmitButton();
        assertThat(submitBtn).isNotNull();
        submitBtn.click();

        wait.until(ExpectedConditions.urlContains("/ui/doctors"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        String body = driver.findElement(By.tagName("body")).getText();
        if (body.contains("Selenium Doctor") || body.contains("Selenium")) {
            assertThat(body).contains("Selenium");
        } else {
            takeScreenshot("doctor-create-missing");
            throw new AssertionError("Created doctor not found in list page. Body snippet: " + body.substring(0, Math.min(300, body.length())));
        }
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
                select.selectByVisibleText(t);
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

    private void takeScreenshot(String name) throws IOException {
        if (!(driver instanceof TakesScreenshot)) return;
        File scr = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File target = new File(System.getProperty("java.io.tmpdir"), name + ".png");
        Files.copy(scr.toPath(), target.toPath());
        System.out.println("Saved screenshot to: " + target.getAbsolutePath());
    }
}

