package ui.pages;

import api.specs.RequestSpec;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.helpers.RetryUtils;
import org.openqa.selenium.Alert;
import ui.elements.BaseElement;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static com.codeborne.selenide.Selenide.switchTo;
import static org.assertj.core.api.Assertions.assertThat;

import static com.codeborne.selenide.Condition.visible;

@SuppressWarnings({"unchecked", "rawtypes", "TypeParameterHidesVisibleType"})
public abstract class BasePage<T extends BasePage> {


    public abstract String url();

//    public abstract T waitForLoadPage();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public T open(Object... params) {
        return Selenide.open(String.format(url(), params), (Class<T>) this.getClass());
    }

    public static void authAsUser(String username, String password) {
        Selenide.open("/");
        RequestSpec.setCookieInBrowser(RequestSpec.fetchSessionCookie(username, password));
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    public T checkAlertMessageAndAccept(String bankAlert) {
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).isEqualTo(bankAlert);
        alert.accept();

        return (T) this;
    }

    protected void retryUntilElementIsDisplayed(SelenideElement element) {
        RetryUtils.retry(
                "Wait until web element is displayed",
                () -> element.isDisplayed() && element.isEnabled(),
                visible -> visible,
                60,
                1000
        );
    }

    public <T extends BaseElement> List<T> generatePageElements(ElementsCollection elementsCollection, Function<SelenideElement, T> constructor) {
        return elementsCollection.stream().map(constructor).toList();
    }

    public void sendKeysIfNotNull(SelenideElement element, String value) {
        if (value != null) {
            element.shouldBe(visible, Duration.ofSeconds(10)).sendKeys(value);
        }
    }
}
