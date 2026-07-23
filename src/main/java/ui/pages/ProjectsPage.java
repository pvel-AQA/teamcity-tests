package ui.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

public class ProjectsPage extends BasePage<ProjectsPage> {

    private static final String LOGIN_PAGE_MARKER = "login.html";

    private final SelenideElement loginUsernameField = $("#username");

    @Override
    public String url() {
        return "/";
    }

    public ProjectsPage checkItIsCorrectPage() {
        webdriver().shouldNotHave(urlContaining(LOGIN_PAGE_MARKER));
        loginUsernameField.shouldNot(exist);
        return this;
    }
}
