package ui;

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

    private static String browserName;

    @BeforeAll
    public static void setupSelenoid() {
        browserName = System.getProperty("browser");
        if (browserName == null || browserName.isEmpty()) {
            Configuration.browser = Config.getProperty("browser");
        }
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = browserName;
        Configuration.browserSize = Config.getProperty("browserSize");
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @BeforeEach
    public void addAllureLabels() {
        Allure.label("browser", browserName);
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }
}
