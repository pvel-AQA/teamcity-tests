package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LoginPage extends BasePage<LoginPage> {

    private final SelenideElement usernameField = $("input#username");
    private final SelenideElement passwordField = $("input#password");
    private final SelenideElement rememberMeCheckbox = $("#remember");
    private final SelenideElement resetPasswordLink = $("span#resetPasswordContainer");
    private final SelenideElement lgoinButton = $(Selectors.byXpath("//input[@value='Log in']"));

    @Override
    public String url() {
        return "/login.html";
    }

    public LoginPage populateUsername(String username) {
        usernameField.shouldBe(Condition.visible);
        usernameField.sendKeys(username);

        return this;
    }

    public LoginPage populatePassword(String password) {
        passwordField.shouldBe(Condition.visible);
        passwordField.sendKeys(password);

        return this;
    }

    public LoginPage clickLoginButton() {
        lgoinButton.shouldBe(Condition.visible);
        lgoinButton.click();

        return this;
    }

    public LoginPage checkUsernameIsEmpty() {
        usernameField.shouldBe(Condition.empty);

        return this;
    }

    public LoginPage login(String username, String password) {
        populateUsername(username);
        populatePassword(password);
        clickLoginButton();

        return this;
    }
}
