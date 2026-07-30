package ui.pages;

import api.specs.RequestSpec;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Alert;
import ui.elements.BaseElement;

import java.util.List;
import java.util.function.Function;

import static com.codeborne.selenide.Selenide.switchTo;
import static org.assertj.core.api.Assertions.assertThat;

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

    public <T extends BaseElement> List<T> generatePageElements(ElementsCollection elementsCollection, Function<SelenideElement, T> constructor) {
        return elementsCollection.stream().map(constructor).toList();
    }
}
