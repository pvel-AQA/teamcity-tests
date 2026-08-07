package ui.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.helpers.RetryUtils;
import org.openqa.selenium.By;
import ui.pages.BasePage;

public abstract class BaseElement {
    protected final SelenideElement element;

    protected BaseElement(SelenideElement element) {
        this.element = element;
    }

    protected SelenideElement find(By selector) { return element.find(selector);}

    protected SelenideElement find(String cssSelector) {return element.find(cssSelector);}

    protected ElementsCollection findAll(By selector) {return element.findAll(selector);}

    protected ElementsCollection findAll(String cssSelector) {return element.findAll(cssSelector);}

    protected void retryUntilElementIsDisplayed(SelenideElement element) {
        RetryUtils.retry(
                "Wait until web element is displayed",
                () -> element.isDisplayed() && element.isEnabled(),
                visible -> visible,
                60,
                1000
        );
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }
}
