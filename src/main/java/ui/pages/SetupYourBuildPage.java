package ui.pages;

import api.enums.buildconfiguration.BuildConfigDropdown;
import api.enums.buildconfiguration.BuildConfigTypeDropdown;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import ui.enums.errors.BuildConfigErrorMessage;

import static com.codeborne.selenide.Selenide.$;

public class SetupYourBuildPage extends BasePage<SetupYourBuildPage> {

    private final SelenideElement buildConfigurationButton = $(Selectors.byXpath("//button/*/span[text()='Build configuration']"));
    private final SelenideElement buildConfigurationDropdown = $(Selectors.byXpath("//span[@data-test='ring-select']/span/button"));
    private final SelenideElement nameTextbox = $(Selectors.byXpath("//input[@aria-label='Name']"));
    private final SelenideElement showMoreButton = $(Selectors.byXpath("//button[text()='Show more']"));
    private final SelenideElement buildConfigurationTypeDropdown = $(Selectors.byXpath("//label[text()='Build configuration type']/../div[@data-test='ring-select']"));
    private final SelenideElement createButton = $(Selectors.byXpath("//button[@type='submit']"));


    @Override
    public String url() {
        return "/projects/create?projectId=%s&setup=build";
    }

    public SetupYourBuildPage clickBuildConfigurationButton() {
        buildConfigurationButton.shouldBe(Condition.visible);
        buildConfigurationButton.click();

        return this;
    }

    public SetupYourBuildPage clickBuildConfigurationDropdown() {
        buildConfigurationDropdown.shouldBe(Condition.visible);
        buildConfigurationDropdown.click();
        buildConfigurationDropdown.shouldHave(Condition.attribute("aria-expanded", "true"));

        return this;
    }

    public SetupYourBuildPage clickShowMoreButton() {
        showMoreButton.shouldBe(Condition.visible);
        showMoreButton.click();

        return this;
    }

    public SetupYourBuildPage populateNameTextbox(String text) {
        nameTextbox.shouldBe(Condition.visible);
        nameTextbox.sendKeys(text);

        return this;
    }

    public SetupYourBuildPage clickBuildConfigurationTypeDropdown() {
        buildConfigurationTypeDropdown.shouldBe(Condition.visible);
        buildConfigurationTypeDropdown.click();

        return this;
    }

    public SetupYourBuildPage selectOptionFromBuildConfigurationDropdown(BuildConfigDropdown option) {
        clickBuildConfigurationDropdown();

        SelenideElement button = $(Selectors.byXpath("//span[@aria-label='%s']/ancestor::span[@role='button']"
                .formatted(option.getDropdownOption())));

        Selenide.executeJavaScript("arguments[0].click();", button);

        return this;
    }

    public SetupYourBuildPage selectOptionFromBuildConfigurationTypeDropdown(BuildConfigTypeDropdown option) {
        clickBuildConfigurationTypeDropdown();

        SelenideElement dropdownOption = $(Selectors.byXpath("//span[@title='%s']/ancestor::button".formatted(option.getValue())))
                .shouldBe(Condition.visible);
        Selenide.executeJavaScript("arguments[0].click();", dropdownOption);

        return this;
    }

    public SetupYourBuildPage clickCreateButton() {
        createButton.shouldBe(Condition.visible);
        createButton.click();

        return this;
    }

    public SetupYourBuildPage checkErrorNotificationAppearsOnCreationWithExistingName(
            BuildConfigErrorMessage buildConfigErrorMessage, String buildConfigName, String projectName) {
        $("[data-test='alert-container']").shouldBe(Condition.visible)
                .shouldHave(Condition.text(
                        buildConfigErrorMessage.getMessage().formatted(buildConfigName, projectName)));

        return this;
    }
}
