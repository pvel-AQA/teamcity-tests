package ui.pages;

import com.codeborne.selenide.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.webdriver;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

public class AdminProjectsPage extends AuthBasePage<AdminProjectsPage> {

    public static final String ADMIN_PROJECTS_URL_MARKER = "admin.html?item=projects";
    private static final String LOGIN_PAGE_MARKER = "login.html";
    public static final String ROOT_PROJECT_ID = "_Root";
    private static final String PROJECTS_TITLE = "Projects";
    private static final Pattern ACTIVE_PROJECTS_COUNT = Pattern.compile("(\\d+)\\s+active projects");

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
    //private final SelenideElement restPageKeywordSearchField = $();
    //private final SelenideElement restPageFilterBtn = $();
    //private final SelenideElement restPageShowArchivedCheckBox = $();
    //private final SelenideElement restPageExpandAllBtn = $();
    //private final SelenideElement restPageCollapseAllBtn = $();
    //private final SelenideElement restPageRootProjectHeader = $();
    private final SelenideElement restPageRootProjectContentList = $("#adminOverview");
    private final ElementsCollection projectSettingsLinks =
            $$("#adminOverview a[href*='editProject.html'][href*='projectId=']");

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

    private String projectIdFrom(String settingsHref) {
        return settingsHref.replaceAll(".*projectId=([^&]+).*", "$1");
    }
}
