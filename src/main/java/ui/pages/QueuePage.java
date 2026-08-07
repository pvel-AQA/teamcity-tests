package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.enums.BuildStatus;
import common.helpers.RetryUtils;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class QueuePage extends BasePage<QueuePage> {

    private final SelenideElement pauseBuildButton = $(Selectors.byXpath("//button[text()='Pause Build Queue']"));
    private final SelenideElement confirmBuildButton = $("#ChangeQueueStateSubmitButton");
    private final SelenideElement resumeBuildButton = $(Selectors.byXpath("//button[text()='Resume Build Queue']"));
    private final SelenideElement projectsButton = $(Selectors.byXpath("//span[text()='Projects']"));
    private final SelenideElement buildStateText = $(Selectors.byText("Build queue was paused"));
    private final SelenideElement noBuildsInQueueText = $(Selectors.byXpath("//h2[contains(@class,'ring-heading')]"));

    public QueuePage pauseBuilds() {
        pauseBuildButton.click();
        confirmBuildButton.shouldBe(Condition.visible).click();
        return this;
    }

    public QueuePage resumeBuilds() {
        resumeBuildButton.click();
        confirmBuildButton.shouldBe(Condition.visible).click();
        return this;
    }

    public ProjectsPage openProjectsPageViaMenu() {
        projectsButton.click();
        return getPage(ProjectsPage.class);
    }

    @Override
    public String url() {
        return "/queue";
    }

    public QueuePage checkBuildStatusText(BuildStatus buildStatus) {
        buildStateText.shouldBe(Condition.visible).shouldHave(Condition.text(buildStatus.getValue()));
        return this;
    }

    public QueuePage checkPausedStatusIsHiddenForBuild() {

        RetryUtils.retry(
                "Wait until paused status is hidden for build",
                () -> {
                    buildStateText.shouldBe(Condition.hidden);
                    return true;
                },
                Boolean::booleanValue,
                20,
                1000
        );
        return this;
    }

    public QueuePage checkThereIsNoBuildsInQueue() {
        noBuildsInQueueText.shouldBe(Condition.visible).shouldHave(Condition.text("No builds in queue"));
        return this;
    }
}
