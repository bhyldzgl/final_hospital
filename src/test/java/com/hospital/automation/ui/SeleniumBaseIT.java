package com.hospital.automation.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class SeleniumBaseIT {

    @LocalServerPort
    protected int port;

    protected WebDriver driver;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--window-size=1400,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    protected void open(String path) {
        driver.get(baseUrl() + path);
    }

    protected WebElement findRowContaining(String text) {
        for (WebElement row : driver.findElements(By.cssSelector("table tbody tr"))) {
            if (row.getText().contains(text)) return row;
        }
        return null;
    }

    protected void acceptConfirmIfPresent() {
        try {
            driver.switchTo().alert().accept();
        } catch (NoAlertPresentException ignored) {}
    }
}
