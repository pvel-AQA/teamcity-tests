package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EditBuildConfigurationPage extends BasePage<EditBuildConfigurationPage> {

    private final SelenideElement runButton = $(Selectors.byXpath("//button[text()='Run']"));

    @Override
    public String url() {
        return "/admin/editBuildRunners.html?id=buildType%%3A%s";
    }

    public BuildRunPage runBuild() {
        runButton.click();
        return getPage(BuildRunPage.class);
    }
}
