package ui.pages;

import com.codeborne.selenide.*;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

public class AdminProjectsPage extends AuthBasePage<AdminProjectsPage> {

    public static final String ADMIN_PROJECTS_URL_MARKER = "admin.html?item=projects";
    private static final String LOGIN_PAGE_MARKER = "login.html";
    private static final String INCLUDE_ARCHIVED_URL_MARKER = "includeArchived=true";
    public static final String ROOT_PROJECT_ID = "_Root";
    public static final String NO_MATCHES_MESSAGE = "No active projects and build configurations match the query.";
    public static final String FILTER_HINT = "Filter projects, build configurations and pipelines by name, ID or description";
    private static final String PROJECTS_TITLE = "Projects";
    //private static final String PROJECT_ARCHIVED = "Projects";
    private static final String KEYWORD_URL_PARAMETER = "keyword=";
    private static final Pattern ACTIVE_PROJECTS_COUNT = Pattern.compile("(\\d+)\\s+active projects");
    private static final Pattern PROJECT_DEPTH = Pattern.compile("depth-(\\d+)");
    private static final Pattern BUILD_TYPE_ID = Pattern.compile("id=buildType:([^&]+)");
    private static final Duration EXPAND_TIMEOUT = Duration.ofSeconds(20);

    private final SelenideElement createProjectLink = $("p.createProject a[href*='/projects/create']");
    private final SelenideElement accessDeniedMessage = $(Selectors.byXpath(
            "//*[contains(normalize-space(.), 'Access denied')"
                    + " or contains(normalize-space(.), 'do not have enough permissions')]"));
    private final SelenideElement leftPanelProjects = $("a[href*='item=projects']");
    private final SelenideElement SearchByBuildNumberField = $("#headerSearchField");
    private final SelenideElement Projects = $("div.restPageTitleWrapper");
    //private final SelenideElement restPageInstallBuildAgentsLink = $();
    //private final SelenideElement restPageCreateProjectBtn = $();
    private final SelenideElement ProjectsDescription = $("div.descr");
    private final SelenideElement KeywordSearchField = $("#keyword");
    private final SelenideElement FilterBtn = $("input[name='submitFilter']");
    private final SelenideElement ResetFilterLink = $("a.reset[title='Reset the filter']");
    private final SelenideElement FilterHint = $("div.actionBar div.smallNote");
    private final SelenideElement AllProjectsBlock = $("#all-projects");
    private final SelenideElement restPageShowArchivedCheckBox = $("#includeArchived");
    private final SelenideElement ShowArchivedLabel = $("label[for='includeArchived']");
    private final SelenideElement ExpandAllBtn = $("a[title='Expand All']");
    private final SelenideElement CollapseAllBtn = $("a[title='Collapse All']");
    //private final SelenideElement restPageRootProjectHeader = $();
    private final SelenideElement RootProjectContentList = $("#adminOverview");
    private final ElementsCollection projectSettingsLinks =
            $$("#adminOverview a[href*='editProject.html'][href*='projectId=']");
    private final ElementsCollection buildConfigurationLinks =
            $$("#adminOverview a[href*='editBuild.html'][href*='id=buildType:']");

    @Override
    public String url() {
        return "/admin/admin.html?item=projects";
    }

    public AdminProjectsPage checkItIsCorrectPage() {
        webdriver().shouldHave(urlContaining(ADMIN_PROJECTS_URL_MARKER));
        webdriver().shouldNotHave(urlContaining(LOGIN_PAGE_MARKER));
        leftPanelProjects.shouldBe(visible);
        Projects.shouldBe(visible).shouldHave(Condition.text(PROJECTS_TITLE));
        ProjectsDescription.shouldBe(visible);
        RootProjectContentList.shouldBe(visible);
        SearchByBuildNumberField.shouldBe(visible);
        createProjectLink.shouldBe(Condition.visible);
        checkHeaderIsVisible();
        checkFilterIsAvailable();
        return this;
    }

    public AdminProjectsPage checkHeaderIsVisible() {
        header.shouldBe(Condition.visible);
        return this;
    }

    public Map<String, String> getDisplayedProjects() {
        RootProjectContentList.shouldBe(visible);
        return projectSettingsLinks.shouldHave(CollectionCondition.sizeGreaterThan(0))
                .asFixedIterable().stream()
                .map(link -> Map.entry(
                        projectIdFrom(link.getAttribute("href")),
                        link.getAttribute("textContent").trim()))
                .filter(project -> !ROOT_PROJECT_ID.equals(project.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public int getActiveProjectsCountFromDescription() {
        String description = ProjectsDescription.shouldBe(visible)
                .shouldHave(matchText(ACTIVE_PROJECTS_COUNT.pattern()))
                .getText();

        Matcher matcher = ACTIVE_PROJECTS_COUNT.matcher(description);
        if (!matcher.find()) {
            throw new IllegalStateException("No projects count in the page description: " + description);
        }
        return Integer.parseInt(matcher.group(1));
    }

    public int getDisplayedProjectsCount() {
        return getDisplayedProjects().size();
    }

    public AdminProjectsPage checkFilterIsAvailable() {
        KeywordSearchField.shouldBe(visible).shouldBe(empty);
        FilterBtn.shouldBe(visible);
        FilterHint.shouldBe(visible).shouldHave(Condition.text(FILTER_HINT));
        ShowArchivedLabel.shouldBe(visible);
        return this;
    }

    public AdminProjectsPage enterKeywordAndClickFilterButton(String keyword) {
        KeywordSearchField.shouldBe(visible).sendKeys(keyword);
        FilterBtn.shouldBe(visible).click();
        webdriver().shouldHave(urlContaining(KEYWORD_URL_PARAMETER + keyword));
        return this;
    }

    public String getFilterKeyword() {
        return KeywordSearchField.shouldBe(visible).getValue();
    }

    public AdminProjectsPage checkNothingMatchesTheFilter() {
        AllProjectsBlock.shouldBe(visible).shouldHave(Condition.text(NO_MATCHES_MESSAGE));
        RootProjectContentList.shouldNot(exist);
        return this;
    }

    public AdminProjectsPage resetFilter() {
        ResetFilterLink.shouldBe(visible).click();
        KeywordSearchField.shouldBe(visible).shouldBe(empty);
        RootProjectContentList.shouldBe(visible);
        return this;
    }

    public AdminProjectsPage clickShowArchivedProjects() {
        ShowArchivedLabel.shouldBe(visible).click();
        webdriver().shouldHave(urlContaining(INCLUDE_ARCHIVED_URL_MARKER));
        RootProjectContentList.shouldBe(visible);
        return this;
    }

    public AdminProjectsPage doNotShowArchivedProjects() {
        ShowArchivedLabel.shouldBe(visible).click();
        webdriver().shouldNotHave(urlContaining(INCLUDE_ARCHIVED_URL_MARKER));
        RootProjectContentList.shouldBe(visible);
        return this;
    }

    public AdminProjectsPage expandAllProjects() {
        ExpandAllBtn.shouldBe(visible).click();
        return this;
    }

    public AdminProjectsPage checkProjectIsVisible(String projectId, String projectName) {
        getProjectSettingsLinkElementWithID(projectId).shouldBe(visible, EXPAND_TIMEOUT).shouldHave(exactText(projectName));
        return this;
    }

    //redo and update test chain
    public boolean isProjectMarkedArchived(String projectId) {
        return getProjectNameFromTheList(projectId).$("span.archived_project").exists();
    }

    // how to write more clear code? what is matchers? to put it into a separate variable
    public int getProjectDepth(String projectId) {
        String cssClasses = getProjectNameFromTheList(projectId).shouldBe(visible).getAttribute("class");

        Matcher matcher = PROJECT_DEPTH.matcher(cssClasses == null ? "" : cssClasses);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "No depth-N class on the row of project " + projectId + ": " + cssClasses);
        }
        return Integer.parseInt(matcher.group(1));
    }


    private SelenideElement getProjectSettingsLinkElementWithID(String projectId) {
        return $("#adminOverview a[href$='projectId=" + projectId + "']");
    }

    //get Project Name from the list
    private SelenideElement getProjectNameFromTheList(String projectId) {
        return getProjectSettingsLinkElementWithID(projectId).ancestor(".project_name");
    }

    private String projectIdFrom(String settingsHref) {

        return settingsHref.replaceAll(".*projectId=([^&]+).*", "$1");
    }
}
