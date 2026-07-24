package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditBuildGeneralPage extends BasePage<EditBuildGeneralPage> {
    private final SelenideElement buildConfigNameTextbox = $("#name");
    private final SelenideElement buildConfigurationIdTextbox = $("#externalId");

    @Override
    public String url() {
        return "/admin/editBuild.html?id=buildType:%s";
    }

    public String getBuildConfigNameText() {
        buildConfigNameTextbox.shouldBe(Condition.visible);

        return buildConfigNameTextbox.getValue();
    }

    public String getBuildConfigIdText() {
        buildConfigurationIdTextbox.shouldBe(Condition.visible);
        return buildConfigurationIdTextbox.getValue();
    }
}
