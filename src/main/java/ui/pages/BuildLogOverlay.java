package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import common.enums.BuildStatus;
import common.enums.BuildStepCommand;

import static com.codeborne.selenide.Selenide.$;

public class BuildLogOverlay extends BasePage<BuildLogOverlay> {
    private final SelenideElement buildLogHeaderStatusText = $(Selectors.byXpath("//div[contains(@class, 'BuildLogPopupHeader-module__descriptionText')]"));
    private final SelenideElement closeButton = $(Selectors.byXpath("//span[@data-test='ring-icon' and contains(@class, 'ring-dialog-closeIcon')]"));

    public BuildLogOverlay checkLogHeaderStatusIs(BuildStatus buildStatus) {
        buildLogHeaderStatusText.shouldBe(Condition.visible)
                .shouldHave(Condition.text(buildStatus.getValue()));
        return this;
    }

    public BuildLogOverlay checkLogHeaderErrorStatusIs(BuildStepCommand error) {
        String expectedRegex = error.getResultOfCommand() + " \\(Step: .+ \\(Command Line\\)\\) \\(new\\)";
        buildLogHeaderStatusText.shouldBe(Condition.visible)
                .shouldHave(Condition.matchText(expectedRegex));

        return this;
    }

    public BuildRunPage closeOverlay() {
        closeButton.click();
        return getPage(BuildRunPage.class);
    }

    @Override
    public String url() {
        return "";
    }

    public String getBuildRunId() {
        String currentUrl = WebDriverRunner.url();

        String urlWithoutQueryParams = currentUrl.split("\\?")[0];
        String[] urlParts = urlWithoutQueryParams.split("/");
        String buildId = urlParts[urlParts.length - 1];

        return buildId;
    }
}
