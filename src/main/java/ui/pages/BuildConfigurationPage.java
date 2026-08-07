package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class BuildConfigurationPage extends BasePage<BuildConfigurationPage> {

    private SelenideElement settingsButton = $("span[aria-label='Edit settings']");

    @Override
    public String url() {
        return "/buildConfiguration/%s";
    }

    public EditBuildGeneralPage clickSettingsButton() {
        settingsButton.shouldBe(Condition.visible);
        settingsButton.click();

        return getPage(EditBuildGeneralPage.class);
    }
}
