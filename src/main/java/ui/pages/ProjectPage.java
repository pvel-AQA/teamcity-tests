package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.enums.BuildStatus;
import common.helpers.RetryUtils;

import static com.codeborne.selenide.Selenide.$;

public class ProjectPage extends ProjectsPage {

    private final SelenideElement runBuildButton = $(Selectors.byAttribute("data-test", "run-build"));
    private final SelenideElement buildStatusText = $(Selectors.byXpath("//a[contains(@class,'BuildStatusLink')]"));
    private final SelenideElement buildRunDetailsButton = $(Selectors.byXpath("//a[@data-test-build-number-link='true']"));

    public ProjectPage runBuild() {
        runBuildButton.shouldBe(Condition.visible).click();
        return this;
    }

    public ProjectPage checkBuildStatusLinkIs(BuildStatus buildStatus) {
        RetryUtils.retry("Wait until Build Run status is Success",
                buildStatusText::getText,
                value -> value.equalsIgnoreCase(buildStatus.getValue()),
                5,
                5000);

        return this;
    }

    public BuildRunPage openBuild() {
        buildRunDetailsButton.click();
        return getPage(BuildRunPage.class);
    }
}
