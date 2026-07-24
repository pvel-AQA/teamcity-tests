package ui.pages.buildsteps;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.enums.BuildStepsRunners;
import ui.pages.BasePage;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;

public class BuildStepsPage extends BasePage<BuildStepsPage> {

    SelenideElement addBuildStepBtn = $x("//a[@class='btn' and .//span[text()='Add build step']]");
    SelenideElement newBuildStepTitle = $x("//span[contains(text(), 'New Build Step')]");
    SelenideElement searchField = $(Selectors.byPlaceholder("Search for recipes or runners..."));
    ElementsCollection searchResults = $$x("//div[@data-test='build-step-selector-item runner']");

    @Override
    public String url() {
        return "/";
    }

    public BuildStepsPage openBuildStepsTab(String buildConfigId) {
        String formattedUrl = "http://localhost:8111/admin/editBuildRunners.html?id=buildType%%3A%s"
                .formatted(buildConfigId);
        Selenide.open(formattedUrl);
        return new BuildStepsPage();
    }

    public PowerShellStepPage selectPowerShellRunner() {
        return selectRunner(BuildStepsRunners.POWER_SHELL);
    }

    @SuppressWarnings("unchecked")
    protected <T extends BuildStepsPage> T selectRunner(BuildStepsRunners runner) {
        searchField.shouldBe(visible).sendKeys(runner.getDisplayName());
        searchResults.shouldHave(sizeGreaterThan(0));
        SelenideElement target = searchResults
                .filterBy(text(runner.getRunnerType()))
                .first();
        target.shouldBe(visible).click();
        return (T) runner.createPage();
    }

}
