package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import ui.enums.successmessages.UISuccessMessage;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class EditBuildGeneralPage extends EditBuildHeaderPage {
    private final SelenideElement buildConfigNameTextbox = $("#name");
    private final SelenideElement buildConfigurationIdTextbox = $("#externalId");
    private final SelenideElement saveButton = $(".saveButtonsBlock input[value='Save']");
    private final SelenideElement buildConfigAdminActionsMenuButton = $("[data-hint-container-id='build-configuration-admin-actions']");

    public String getBuildConfigNameText() {
        buildConfigNameTextbox.shouldBe(Condition.visible);

        return buildConfigNameTextbox.getValue();
    }

    public String getBuildConfigIdText() {
        buildConfigurationIdTextbox.shouldBe(Condition.visible);
        return buildConfigurationIdTextbox.getValue();
    }

    public EditBuildGeneralPage populateBuildConfigName(String buildConfigName) {
        buildConfigNameTextbox.shouldBe(Condition.visible).clear();
        buildConfigNameTextbox.sendKeys(buildConfigName);

        return this;
    }

    public EditBuildGeneralPage clickSaveButton() {
        saveButton.shouldBe(Condition.visible);
        saveButton.click();

        return this;
    }

    public EditBuildGeneralPage clickBuildConfigAdminActionsMenuButton() {
        buildConfigAdminActionsMenuButton.shouldBe(Condition.visible);
        buildConfigAdminActionsMenuButton.click();

        return this;
    }

    public EditBuildGeneralPage clickDeleteBuildConfigButton() {
        $x("//li[@class='menuItem']//a[text()='Delete...']").shouldBe(Condition.visible)
                .click();

        return this;
    }

    public EditBuildGeneralPage checkSuccessMessageAppearsOnSavingChanges(UISuccessMessage successMessage) {
        $("main[id='main-content-tag'] .successMessage").shouldBe(Condition.visible)
                .shouldHave(Condition.text(successMessage.getMessage()));

        return this;
    }
}
