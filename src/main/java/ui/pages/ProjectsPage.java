package ui.pages;

import com.codeborne.selenide.*;
import common.enums.BuildStatus;
import common.helpers.RetryUtils;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static common.enums.BuildStatus.BUILD_QUEUE_WAS_PAUSED;
import static common.enums.BuildStatus.SUCCESS;

public class ProjectsPage extends BasePage<ProjectsPage> {

    private static final String LOGIN_PAGE_MARKER = "login.html";
    private static final String PROJECTS_ICON_VALUE = "Projects";

    private final SelenideElement loginUsernameField = $("#username");
    private final SelenideElement header = $(Selectors.byXpath("//header[@data-test-main-nav]"));
    private final SelenideElement projectsHeaderIcon = $(Selectors.byXpath("//span[text()='Projects']"));
    private final SelenideElement runBuildButton = $(Selectors.byAttribute("data-test", "run-build"));
//    private final SelenideElement buildPausedStatusText = $(Selectors.byXpath("//span[text()='Build queue was paused']"));
    private final SelenideElement buildStatusText = $(Selectors.byXpath("//a[contains(@class,'BuildStatusLink')]"));
    private final SelenideElement buildRunDetailsButton = $(Selectors.byXpath("//a[@data-test-build-number-link='true']"));


    @Override
    public String url() {
        return "/favorite/projects";
    }

    public ProjectsPage checkItIsCorrectPage() {
        webdriver().shouldNotHave(urlContaining(LOGIN_PAGE_MARKER));
        loginUsernameField.shouldNot(exist);
        return this;
    }

    public ProjectsPage checkHeaderIsVisible() {
        RetryUtils.retry("Wait until header is visible",
                projectsHeaderIcon::getText,
                value -> value.equals(PROJECTS_ICON_VALUE),
                3,
                3000);
        header.shouldBe(Condition.visible);
        return this;
    }

    public ProjectsPage clickOnProject(String projectName) {
        ElementsCollection rows = $$(".ProjectsTreeItem-module__row--h3:has([data-test-itemtype='project'])")
                .shouldHave(CollectionCondition.sizeGreaterThan(0));
        rows.findBy(Condition.text(projectName)).shouldBe(Condition.visible).click();
        return this;
    }

    public ProjectsPage runBuild() {
        runBuildButton.shouldBe(Condition.visible).click();
        return this;
    }

//    public ProjectsPage checkBuildStatusIsPaused() {
//        buildPausedStatusText.shouldBe(Condition.visible).shouldHave(Condition.text(BUILD_QUEUE_WAS_PAUSED.getValue()));
//        return this;
//    }

    public ProjectsPage checkBuildStatusLinkIs(BuildStatus buildStatus) {
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
