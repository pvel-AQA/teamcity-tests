package ui.base;

import base.BaseTest;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.configs.Config;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.model.Label;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

public class BaseUiTest extends BaseTest {

    private String browser;

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browserSize = Config.getProperty("browserSize");
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @BeforeEach
    @Step("Setup browser: {browser}")
    public void setupBeforeEach() {
        browser = System.getProperty("browser");
        if (browser == null || browser.isBlank()) {
            Configuration.browser = Config.getProperty("browser");
        } else {
            Configuration.browser = browser;
        }

        Allure.label("browser", browser);
        Allure.getLifecycle().updateTestCase(testResult -> {
            testResult.setName(testResult.getName() + " [" + browser + "]");
        });
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }
}
