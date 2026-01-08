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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PatientSeleniumIT {
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

        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    @Test
    void createPatient_viaUi_showsInList() throws IOException {
        driver.get(baseUrl + "/ui/patients/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        driver.findElement(By.cssSelector("form input[name='firstName']")).sendKeys("Selenium");
        driver.findElement(By.cssSelector("form input[name='lastName']")).sendKeys("Patient");

        List<WebElement> nat = driver.findElements(By.cssSelector("form input[name='nationalId']"));
        if (!nat.isEmpty()) nat.get(0).sendKeys("12345678901");

        List<WebElement> birth = driver.findElements(By.cssSelector("form input[name='birthDate']"));
        if (!birth.isEmpty()) {
            String dateStr = LocalDate.now().minusYears(30).format(DateTimeFormatter.ISO_DATE);
            birth.get(0).clear();
            if (driver instanceof JavascriptExecutor js) {
                js.executeScript(
                        "arguments[0].value = arguments[1];" +
                                "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                        birth.get(0), dateStr
                );
            } else {
                birth.get(0).sendKeys(dateStr);
            }
        }

        List<WebElement> phone = driver.findElements(By.cssSelector("form input[name='phone']"));
        if (!phone.isEmpty()) phone.get(0).sendKeys("+905551112233");

        List<WebElement> addr = driver.findElements(By.cssSelector("form textarea[name='address'], form input[name='address']"));
        if (!addr.isEmpty()) addr.get(0).sendKeys("Test address");

        WebElement submitBtn = findSubmitButton();
        assertThat(submitBtn).isNotNull();
        wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
        submitBtn.click();

        wait.until(d -> {
            String u = d.getCurrentUrl();
            return u.equals(baseUrl + "/ui/patients") || u.equals(baseUrl + "/ui/patients/");
        });
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        String body = driver.findElement(By.tagName("body")).getText();
        if (!(body.contains("Selenium Patient") || body.contains("Selenium"))) {
            takeScreenshot("patient-create-missing");
            throw new AssertionError("Created patient not found in list page. Body snippet: " +
                    body.substring(0, Math.min(300, body.length())));
        }

        assertThat(body).contains("Selenium");
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
