package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.RetryUtils;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.back;
import static java.lang.Thread.sleep;

public class CreateBuildConfigurationPage extends BasePage<CreateBuildConfigurationPage> {

    private final SelenideElement skipButton = $(Selectors.byText("Skip"));

    public EditProjectPage clickSkipButton() {
        retryUntilElementIsDisplayed(skipButton);
        skipButton.click();
        return getPage(EditProjectPage.class);
    }


    @Override
    public String url() {
        return "";
    }
}
