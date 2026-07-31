package ui.base;

import base.BaseTest;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.configs.Config;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.browser = ui.base.SelenoidDriverProvider.class.getName();

        //Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        //Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");

        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }
}
