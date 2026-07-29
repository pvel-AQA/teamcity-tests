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
    private final SelenideElement restPageSearchByBuildNumberField = $("#headerSearchField");
    private final SelenideElement restPageProjects = $("div.restPageTitleWrapper");
    //private final SelenideElement restPageInstallBuildAgentsLink = $();
    //private final SelenideElement restPageCreateProjectBtn = $();
    private final SelenideElement restPageProjectsDescription = $("div.descr");
    private final SelenideElement restPageKeywordSearchField = $("#keyword");
    private final SelenideElement restPageFilterBtn = $("input[name='submitFilter']");
    private final SelenideElement restPageResetFilterLink = $("a.reset[title='Reset the filter']");
    private final SelenideElement restPageFilterHint = $("div.actionBar div.smallNote");
    private final SelenideElement restPageAllProjectsBlock = $("#all-projects");
    private final SelenideElement restPageShowArchivedCheckBox = $("#includeArchived");
    private final SelenideElement restPageShowArchivedLabel = $("label[for='includeArchived']");
    private final SelenideElement restPageExpandAllBtn = $("a[title='Expand All']");
    private final SelenideElement restPageCollapseAllBtn = $("a[title='Collapse All']");
    //private final SelenideElement restPageRootProjectHeader = $();
    private final SelenideElement restPageRootProjectContentList = $("#adminOverview");
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
        restPageProjects.shouldBe(visible).shouldHave(Condition.text(PROJECTS_TITLE));
        restPageProjectsDescription.shouldBe(visible);
        restPageRootProjectContentList.shouldBe(visible);
        restPageSearchByBuildNumberField.shouldBe(visible);
        createProjectLink.shouldBe(Condition.visible);

        return this;
    }

    public AdminProjectsPage checkHeaderIsVisible() {
        header.shouldBe(Condition.visible);
        return this;
    }

    public Map<String, String> getDisplayedProjects() {
        restPageRootProjectContentList.shouldBe(visible);
        return projectSettingsLinks.shouldHave(CollectionCondition.sizeGreaterThan(0))
                .asFixedIterable().stream()
                .map(link -> Map.entry(
                        projectIdFrom(link.getAttribute("href")),
                        link.getAttribute("textContent").trim()))
                .filter(project -> !ROOT_PROJECT_ID.equals(project.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public int getActiveProjectsCountFromDescription() {
        String description = restPageProjectsDescription.shouldBe(visible)
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

    public AdminProjectsPage showArchivedProjects() {
        restPageShowArchivedLabel.shouldBe(visible).click();
        webdriver().shouldHave(urlContaining(INCLUDE_ARCHIVED_URL_MARKER));
        restPageRootProjectContentList.shouldBe(visible);
        return this;
    }

    public AdminProjectsPage checkFilterIsAvailable() {
        restPageKeywordSearchField.shouldBe(visible).shouldBe(empty);
        restPageFilterBtn.shouldBe(visible);
        restPageFilterHint.shouldBe(visible).shouldHave(exactText(FILTER_HINT));
        return this;
    }

    public AdminProjectsPage filterByKeyword(String keyword) {
        restPageKeywordSearchField.shouldBe(visible).setValue(keyword);
        restPageFilterBtn.shouldBe(visible).click();
        webdriver().shouldHave(urlContaining(KEYWORD_URL_PARAMETER + keyword));
        restPageAllProjectsBlock.shouldBe(visible);
        return this;
    }

    public AdminProjectsPage resetFilter() {
        restPageResetFilterLink.shouldBe(visible).click();
        restPageKeywordSearchField.shouldBe(visible).shouldBe(empty);
        restPageRootProjectContentList.shouldBe(visible);
        return this;
    }

    public String getFilterKeyword() {
        return restPageKeywordSearchField.shouldBe(visible).getValue();
    }

    public Map<String, String> getDisplayedBuildConfigurations() {
        restPageAllProjectsBlock.shouldBe(visible);
        return buildConfigurationLinks.asFixedIterable().stream()
                .map(link -> Map.entry(
                        buildTypeIdFrom(link.getAttribute("href")),
                        link.$("span.build_type_name_inner").getAttribute("textContent").trim()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public AdminProjectsPage checkNothingMatchesTheFilter() {
        restPageAllProjectsBlock.shouldBe(visible).shouldHave(text(NO_MATCHES_MESSAGE));
        restPageRootProjectContentList.shouldNot(exist);
        return this;
    }

    public AdminProjectsPage expandAllProjects() {
        restPageExpandAllBtn.shouldBe(visible).click();
        return this;
    }

    public AdminProjectsPage checkProjectIsVisible(String projectId, String projectName) {
        projectSettingsLink(projectId).shouldBe(visible, EXPAND_TIMEOUT).shouldHave(exactText(projectName));
        return this;
    }

    public boolean isProjectMarkedArchived(String projectId) {
        return projectNameCell(projectId).$("span.archived_project").exists();
    }

    public int getProjectDepth(String projectId) {
        String cssClasses = projectNameCell(projectId).shouldBe(visible).getAttribute("class");

        Matcher matcher = PROJECT_DEPTH.matcher(cssClasses == null ? "" : cssClasses);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "No depth-N class on the row of project " + projectId + ": " + cssClasses);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private SelenideElement projectSettingsLink(String projectId) {
        return $("#adminOverview a[href$='projectId=" + projectId + "']");
    }

    private SelenideElement projectNameCell(String projectId) {
        return projectSettingsLink(projectId).ancestor(".project_name");
    }

    private String projectIdFrom(String settingsHref) {
        return settingsHref.replaceAll(".*projectId=([^&]+).*", "$1");
    }

    private String buildTypeIdFrom(String settingsHref) {
        Matcher matcher = BUILD_TYPE_ID.matcher(settingsHref == null ? "" : settingsHref);
        if (!matcher.find()) {
            throw new IllegalStateException("No build configuration id in the settings link: " + settingsHref);
        }
        return matcher.group(1);
    }
}
