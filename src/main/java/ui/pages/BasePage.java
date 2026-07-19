package ui.pages;

import api.specs.RequestSpec;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ui.elements.BaseElement;

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class BasePage<T extends BasePage> {


    public abstract String url();

//    public abstract T waitForLoadPage();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    /**
     * Fast login: seed the browser session from the API instead of driving the login form.
     * <ol>
     *   <li>open the app so a cookie domain exists,</li>
     *   <li>fetch the {@code TCSESSIONID} cookie via a Basic-auth API call,</li>
     *   <li>inject that cookie into the WebDriver.</li>
     * </ol>
     * After this the SPA treats the browser as already logged in. See ui-testing-guide.md §4.
     */
    public static void authAsUser(String username, String password) {
        Selenide.open("/");
        RequestSpec.setCookieInBrowser(RequestSpec.fetchSessionCookie(username, password));
    }

    public T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    public <T extends BaseElement> List<T> generatePageElements(ElementsCollection elementsCollection, Function<SelenideElement, T> constructor) {
        return elementsCollection.stream().map(constructor).toList();
    }
}
