package ui.pages.buildsteps;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.enums.BuildStepsRunners;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import ui.pages.BasePage;

import java.time.Duration;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;

@Getter
public class BuildStepsPage extends BasePage<BuildStepsPage> {

    private static final String BUILD_STEP_ROW_XPATH = "//tr[@class='editBuildStepRow'][.//strong[text()='%s']]";

    private final SelenideElement addBuildStepBtn = $x("//a[@class='btn' and .//span[text()='Add build step']]");
    private final SelenideElement newBuildStepTitle = $x("//span[contains(text(), 'New Build Step')]");
    private final SelenideElement searchField = $(Selectors.byPlaceholder("Search for recipes or runners..."));
    private final ElementsCollection searchResults = $$x("//div[@data-test='build-step-selector-item runner']");

    @Override
    public String url() {
        return "/admin/editBuildRunners.html?id=buildType:%s";
    }

    public PowerShellStepPage selectPowerShellRunner() {
        return selectRunner(BuildStepsRunners.POWER_SHELL);
    }

    @SuppressWarnings("unchecked")
    public <T extends BuildStepsPage> T selectRunner(BuildStepsRunners runner) {
        addBuildStepBtn.shouldBe(visible).click();
        newBuildStepTitle.shouldHave(appear, Duration.ofSeconds(6));
        searchField.shouldBe(visible).sendKeys(runner.getDisplayName());
        searchResults.shouldHave(sizeGreaterThan(0));
        SelenideElement target = searchResults
                .filterBy(text(runner.getRunnerType()))
                .first();
        target.shouldBe(visible).click();
        return (T) runner.createPage();
    }

    public void selectBuildStepByName(String buildStepName) {
        $x(BUILD_STEP_ROW_XPATH.formatted(buildStepName)).shouldBe(visible).click();
    }

    public BuildStepsPage isBuildStepVisible(String buildStepName) {
        Assertions.assertThat($x(BUILD_STEP_ROW_XPATH.formatted(buildStepName)).is(visible, Duration.ofSeconds(5)))
                .isTrue()
                .as("Created build step should be displayed");
        return this;
    }

}
