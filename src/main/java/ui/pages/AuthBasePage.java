package ui.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public abstract class AuthBasePage<T extends AuthBasePage> extends BasePage<T>{
    public SelenideElement header = $(".ring-header-headerVertical");
    //public SelenideElement headerHomeLink = $("");
    //public SelenideElement headerScrollableSection = $(Selectors.byXpath("//header[@data-test-main-nav]"));
    //public SelenideElement headerBottom = $("");
}
