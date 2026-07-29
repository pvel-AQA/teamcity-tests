package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.RetryUtils;
import ui.elements.ProjectElement;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

public class ProjectsPage extends BasePage<ProjectsPage> {

    private ProjectElement projectElement;

    private static final String LOGIN_PAGE_MARKER = "login.html";
    private static final String PROJECTS_ICON_VALUE = "Projects";

    private final SelenideElement loginUsernameField = $("#username");
    private final SelenideElement header = $(Selectors.byXpath("//header[@data-test-main-nav]"));
    private final SelenideElement projectsHeaderIcon = $(Selectors.byXpath("//span[text()='Projects']"));

    @Override
    public String url() {
        return "";
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

    public ProjectElement getProjectsSideBar() {
        return new ProjectElement($(".SidebarPanel-module__sidebar--G7"));
    }
}
