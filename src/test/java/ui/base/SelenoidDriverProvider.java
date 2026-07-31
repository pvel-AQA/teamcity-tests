package ui.base;

import com.codeborne.selenide.WebDriverProvider;
import common.configs.Config;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

public class SelenoidDriverProvider implements WebDriverProvider {
    @Override
    public WebDriver createDriver(Capabilities capabilities) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.setCapability("selenoid:options", Map.of(
                "enableVNC", true,
                "enableLog", true
        ));

        options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

        try {
            String remoteUrl = Config.getProperty("uiRemote");
            RemoteWebDriver driver = new RemoteWebDriver(new URL(remoteUrl), options);

            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Selenoid URL configuration", e);
        }
    }
}
