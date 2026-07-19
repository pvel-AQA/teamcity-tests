package ui;

import base.BaseTest;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.configs.Config;
import common.extensions.AdminSessionExtension;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

/**
 * Base class for all UI tests. Configures Selenide/Selenoid + Allure, and wires the
 * {@link AdminSessionExtension} that powers {@code @AdminSession} fast login. Extends
 * {@link BaseTest} so UI tests also inherit the API-side setup (entity storage, cleanup, softly).
 */
@ExtendWith(AdminSessionExtension.class)
public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }
}
