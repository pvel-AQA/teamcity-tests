package ui.base;

import base.BaseTest;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.configs.Config;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());


        // 1. Создаем объект настроек Chrome и добавляем критически важные флаги
        org.openqa.selenium.chrome.ChromeOptions chromeOptions = new org.openqa.selenium.chrome.ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");               // Отключает песочницу внутри Docker
        chromeOptions.addArguments("--disable-dev-shm-usage");    // Решает проблему с нехваткой памяти /dev/shm
        chromeOptions.addArguments("--disable-gpu");              // Отключает аппаратное ускорение графики

        // 2. Мержим наши настройки Chrome в общие Browser Capabilities Selenide
        Configuration.browserCapabilities.merge(chromeOptions);

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }
}
