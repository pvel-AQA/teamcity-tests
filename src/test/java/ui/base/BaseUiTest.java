package ui.base;

import base.BaseTest;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.configs.Config;
//import common.extensions.ScreenshotOnFailureExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

//@ExtendWith(ScreenshotOnFailureExtension.class)
public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");

        SelenideLogger.addListener("AllureSelenide",
                new AllureSelenide()
                        .screenshots(true)
                        .savePageSource(true)
        );

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @BeforeEach
    public void setupAllureBrowserContext(TestInfo testInfo) {
        String browser = Configuration.browser;

        String displayName = testInfo.getDisplayName() + " [" + browser.toUpperCase() + "]";

        Allure.getLifecycle().updateTestCase(testCase -> {
            testCase.setName(displayName);
            testCase.setHistoryId(testCase.getHistoryId() + "-" + browser);
        });

        Allure.parameter("Browser", browser);
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }
}
