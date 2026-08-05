package ui.base;

import base.BaseTest;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.configs.Config;
import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupSelenoid() {
        String browser = System.getProperty("browser", Config.getProperty("browser"));
        Configuration.remote = System.getProperty("uiRemote", Config.getProperty("uiRemote"));
        Configuration.baseUrl = System.getProperty("uiBaseUrl", Config.getProperty("uiBaseUrl"));
        Configuration.browser = browser;
        Configuration.browserSize = Config.getProperty("browserSize");

        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

//    @BeforeEach
//    public void setupTestAllureParams() {
//        // Записываем параметр именно перед СТАРТОМ КАЖДОГО ТЕСТА
//        Allure.parameter("Browser", Configuration.browser);
//    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }
}
