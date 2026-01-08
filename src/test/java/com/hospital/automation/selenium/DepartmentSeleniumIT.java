package com.hospital.automation.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DepartmentSeleniumIT {

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
    void createDepartment_viaUi_showsInList() throws IOException {
        driver.get(baseUrl + "/ui/departments/new");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")));

        driver.findElement(By.cssSelector("form input[name='name']")).sendKeys("Selenium Department");

        WebElement submitBtn = findSubmitButton();
        assertThat(submitBtn).isNotNull();
        submitBtn.click();

        wait.until(ExpectedConditions.urlContains("/ui/departments"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        String body = driver.findElement(By.tagName("body")).getText();
        if (body.contains("Selenium Department")) {
            assertThat(body).contains("Selenium Department");
        } else {
            takeScreenshot("department-create-missing");
            throw new AssertionError("Created department not found in list page. Body snippet: " + body.substring(0, Math.min(300, body.length())));
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

