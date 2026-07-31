package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import common.enums.BuildStatus;
import common.enums.BuildStepCommand;
import common.helpers.RetryUtils;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class BuildRunPage extends BasePage<BuildRunPage> {

    private static final String RUNNING_EXPECTED_TEXT = "Running";

    private final SelenideElement buildStatusHeader = $(Selectors.byXpath("//div[contains(@class, 'Description-module__text')]"));
    private final SelenideElement buildStatusBadge = $(Selectors.byXpath("//div[contains(@class, 'StatusBadge-module__status')]"));
    private final SelenideElement timelineButton = $(Selectors.byAttribute("data-hint-container-id", "buildlog-timeline-button"));
    private final SelenideElement runningStepText = $(Selectors.byXpath("//span[contains(@class, 'RunningStep-module__wrapper')]"));
    private final SelenideElement stopBuildButton = $(Selectors.byAttribute("title", "Stop build..."));
    private final SelenideElement confirmStopButton = $(Selectors.byAttribute("value", "Stop"));
    private final SelenideElement timelineStatus = $(Selectors.byXpath("//div[contains(@class, 'BuildLogTimeline-module__timeline')]"));
    private final SelenideElement buildLogMessages = $(Selectors.byXpath("//div[contains(@class, 'BuildLogRunningMessages-module')]"));


    @Override
    public String url() {
        return "/buildConfiguration/%s/%s";
    }


    public BuildRunPage checkBuildStatusHeaderIs(BuildStatus buildStatus) {
        buildStatusHeader.shouldBe(Condition.visible).shouldHave(Condition.text(buildStatus.getValue()));
        return this;
    }

    public BuildRunPage checkTimeLineStatusIs(BuildStatus buildStatus) {
        timelineStatus.shouldBe(Condition.visible)
                .shouldHave(Condition.text(buildStatus.getValue()));
        return this;
    }

    public BuildRunPage checkStatusIndicatorIs(BuildStatus buildStatus) {
        runningStepText.shouldBe(Condition.visible)
                .shouldHave(Condition.text(buildStatus.getValue()));
        return this;
    }

    public BuildLogOverlay openLogOverlayViaTimeline() {
        timelineButton.click();
        return getPage(BuildLogOverlay.class);
    }

    public BuildLogOverlay openLogOverlayViaBuildLogMessages() {
        buildLogMessages.click();
        return getPage(BuildLogOverlay.class);
    }


    public BuildRunPage waitUntilStatusBecomes(BuildStatus buildStatus) {
        return checkStatus(buildStatus, buildStatusHeader);
    }

    public BuildRunPage checkStatusBadgeIs(BuildStatus buildStatus) {
        return checkStatus(buildStatus, buildStatusBadge);
    }

    private BuildRunPage checkStatus(BuildStatus buildStatus, SelenideElement element) {
        RetryUtils.retry(
                "Wait until status of Build Run is correct",
                element::getText,
                value -> value.equalsIgnoreCase(buildStatus.getValue()),
                20,
                1000
        );
        return this;
    }

    public BuildRunPage waitUntilErrorStatusBecomes(BuildStepCommand command) {
        String expectedRegex = command.getResultOfCommand() + " \\(Step: .+ \\(Command Line\\)\\) \\(new\\)";

        RetryUtils.retry(
                "Wait until status of Build Run is failed",
                () -> buildStatusHeader.getText().trim(),
                value -> value.matches(expectedRegex),
                20,
                1000
        );
        return this;
    }

    public BuildRunPage stopBuildRun() {
        RetryUtils.retry("Wait until running appears",
                () -> $x("//*[@class='RunningStep-module__wrapper--t4']").shouldBe(Condition.visible).getText(),
                text -> text.equals(RUNNING_EXPECTED_TEXT),
                3,
                3000
                );
        stopBuildButton.shouldBe(Condition.visible).click();
        $(Selectors.byAttribute("value", "Stop")).click();

        return this;
    }

    public String getBuildRunId() {
        String currentUrl = WebDriverRunner.url();

        String[] urlParts = currentUrl.split("/");
        String buildId = urlParts[urlParts.length - 1];

        return buildId;
    }
}
