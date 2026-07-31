package ui;

import api.generators.RandomGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Test;
import ui.base.BaseUiTest;
import ui.pages.AdminProjectsPage;

import java.util.*;
import java.util.stream.Collectors;

public class AdminProjectsTest extends BaseUiTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesOwnCreatedProjectsOnAdminPageTest() {
        ProjectResponse firstProject =
                UserSteps.createProjectWithExtension(RandomGenerator.generate(ProjectRequest.class));
        ProjectResponse secondProject =
                UserSteps.createProjectWithExtension(RandomGenerator.generate(ProjectRequest.class));

        Map<String, String> expectedProjects = Map.of(
                firstProject.getId(), firstProject.getName(),
                secondProject.getId(), secondProject.getName());

        Map<String, String> uiProjects = new AdminProjectsPage().open()
                .checkItIsCorrectPage()
                .checkHeaderIsVisible()
                .getDisplayedProjects();

        attachProjects("Projects created by this test", expectedProjects);
        attachProjects("UI projects (Admin page)", uiProjects);

        softly.assertThat(uiProjects).containsAllEntriesOf(expectedProjects);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesArchivedProjectOnAdminPageAfterEnablingShowArchivedTest() {
        ProjectResponse archivedProject =
                UserSteps.createProjectWithExtension(RandomGenerator.generate(ProjectRequest.class));
        UserSteps.setProjectArchived(archivedProject.getId(), true);

        AdminProjectsPage adminProjectsPage = new AdminProjectsPage().open()
                .checkItIsCorrectPage()
                .checkHeaderIsVisible();

        attachProjects("UI projects before 'Show archived'", adminProjectsPage.getDisplayedProjects());

        softly.assertThat(adminProjectsPage.getDisplayedProjects()).doesNotContainKey(archivedProject.getId());

        adminProjectsPage.showArchivedProjects();

        attachProjects("UI projects after 'Show archived'", adminProjectsPage.getDisplayedProjects());

        softly.assertThat(adminProjectsPage.getDisplayedProjects())
                .containsEntry(archivedProject.getId(), archivedProject.getName());
        softly.assertThat(adminProjectsPage.isProjectMarkedArchived(archivedProject.getId())).isTrue();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesSubProjectNestedUnderItsParentOnAdminPageTest() {
        ProjectResponse parentProject =
                UserSteps.createProjectWithExtension(RandomGenerator.generate(ProjectRequest.class));
        ProjectResponse subProject = UserSteps.createSubProject(parentProject.getId());

        AdminProjectsPage adminProjectsPage = new AdminProjectsPage().open()
                .checkItIsCorrectPage()
                .checkHeaderIsVisible()
                .expandAllProjects()
                .checkProjectIsVisible(parentProject.getId(), parentProject.getName())
                .checkProjectIsVisible(subProject.getId(), subProject.getName());

        attachProjects("Projects created by this test", Map.of(
                parentProject.getId(), parentProject.getName(),
                subProject.getId(), subProject.getName()));

        softly.assertThat(subProject.getParentProjectId()).isEqualTo(parentProject.getId());
        softly.assertThat(adminProjectsPage.getProjectDepth(subProject.getId()))
                .isEqualTo(adminProjectsPage.getProjectDepth(parentProject.getId()) + 1);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminFiltersProjectsByKeywordAtAnyPositionInNameTest() {
        String keyword = TeamCityDataGenerator.generateString("Fltr", 8);

        ProjectResponse keywordAtStart = UserSteps.createProjectWithName(keyword + "AtStartProject");
        ProjectResponse keywordInMiddle = UserSteps.createProjectWithName("Project" + keyword + "InMiddle");
        ProjectResponse keywordAtEnd = UserSteps.createProjectWithName("ProjectEndsWith" + keyword);
        ProjectResponse unrelated =
                UserSteps.createProjectWithName(TeamCityDataGenerator.generateString("UnrelatedProject", 8));

        Map<String, String> expectedMatches = asMap(keywordAtStart, keywordInMiddle, keywordAtEnd);

        AdminProjectsPage adminProjectsPage = openAdminProjectsPage().filterByKeyword(keyword);
        Map<String, String> displayedProjects = adminProjectsPage.getDisplayedProjects();

        attachProjects("UI projects filtered by '" + keyword + "'", displayedProjects);
        attachProjects("Expected matches", expectedMatches);
        attachProjects("Expected non-matches", asMap(unrelated));

        softly.assertThat(adminProjectsPage.getFilterKeyword())
                .as("Keyword field must keep the applied filter")
                .isEqualTo(keyword);
        softly.assertThat(displayedProjects)
                .as("Filter must show every project whose name contains the keyword at any position, "
                        + "and nothing else")
                .containsExactlyInAnyOrderEntriesOf(expectedMatches);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesMatchingSubProjectNestedUnderItsParentWhenFilteringTest() {
        String keyword = TeamCityDataGenerator.generateString("Fltr", 8);

        ProjectResponse parentProject =
                UserSteps.createProjectWithName(TeamCityDataGenerator.generateString("ParentOfMatchingSub", 8));
        ProjectResponse matchingSubProject =
                UserSteps.createSubProject(parentProject.getId(), "Sub" + keyword + "One");

        AdminProjectsPage adminProjectsPage = openAdminProjectsPage().filterByKeyword(keyword);
        Map<String, String> displayedProjects = adminProjectsPage.getDisplayedProjects();

        attachProjects("UI projects filtered by '" + keyword + "'", displayedProjects);
        attachProjects("Projects created by this test", asMap(parentProject, matchingSubProject));

        adminProjectsPage.checkProjectIsVisible(matchingSubProject.getId(), matchingSubProject.getName());

        softly.assertThat(displayedProjects)
                .as("A parent that does not match the keyword must still be shown as the context "
                        + "of its matching sub-project")
                .containsExactlyInAnyOrderEntriesOf(asMap(parentProject, matchingSubProject));
        softly.assertThat(adminProjectsPage.getProjectDepth(matchingSubProject.getId()))
                .as("A matching sub-project must stay nested under its parent in the filtered tree")
                .isEqualTo(adminProjectsPage.getProjectDepth(parentProject.getId()) + 1);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminDoesNotSeeArchivedMatchesWhileShowArchivedIsOffTest() {
        String keyword = TeamCityDataGenerator.generateString("Fltr", 8);

        ProjectResponse activeMatch = UserSteps.createProjectWithName("Active" + keyword + "Project");
        ProjectResponse archivedMatch = UserSteps.createArchivedProjectWithName("Archived" + keyword + "Project");

        AdminProjectsPage adminProjectsPage = openAdminProjectsPage().filterByKeyword(keyword);
        Map<String, String> displayedProjects = adminProjectsPage.getDisplayedProjects();

        attachProjects("UI projects filtered by '" + keyword + "'", displayedProjects);
        attachProjects("Expected match", asMap(activeMatch));
        attachProjects("Expected archived match (hidden until 'Show archived')", asMap(archivedMatch));

        softly.assertThat(displayedProjects)
                .as("Only the active match may be listed while 'Show archived' is off")
                .containsExactlyInAnyOrderEntriesOf(asMap(activeMatch));
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesArchivedMatchesAfterEnablingShowArchivedWhileFilteringTest() {
        String keyword = TeamCityDataGenerator.generateString("Fltr", 8);

        ProjectResponse activeMatch = UserSteps.createProjectWithName("Active" + keyword + "Project");
        ProjectResponse archivedMatch = UserSteps.createArchivedProjectWithName("Archived" + keyword + "Project");

        AdminProjectsPage adminProjectsPage = openAdminProjectsPage()
                .filterByKeyword(keyword)
                .showArchivedProjects();
        Map<String, String> displayedProjects = adminProjectsPage.getDisplayedProjects();

        attachProjects("UI projects filtered by '" + keyword + "' with archived shown", displayedProjects);
        attachProjects("Expected matches", asMap(activeMatch, archivedMatch));

        softly.assertThat(adminProjectsPage.getFilterKeyword())
                .as("Keyword field must keep the applied filter after enabling 'Show archived'")
                .isEqualTo(keyword);
        softly.assertThat(displayedProjects)
                .as("'Show archived' must add the matching archived project to the filtered result")
                .containsExactlyInAnyOrderEntriesOf(asMap(activeMatch, archivedMatch));
        softly.assertThat(adminProjectsPage.isProjectMarkedArchived(archivedMatch.getId()))
                .as("Project %s must be marked as archived", archivedMatch.getId())
                .isTrue();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesNoMatchesMessageWhenKeywordMatchesNothingTest() {
        ProjectResponse existingProject =
                UserSteps.createProjectWithExtension(RandomGenerator.generate(ProjectRequest.class));

        AdminProjectsPage adminProjectsPage = openAdminProjectsPage();
        attachProjects("UI projects before filtering", adminProjectsPage.getDisplayedProjects());

        adminProjectsPage
                .checkProjectIsVisible(existingProject.getId(), existingProject.getName())
                .filterByKeyword(TeamCityDataGenerator.generateString("NoSuchProject", 8))
                .checkNothingMatchesTheFilter();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesFilteredOutProjectsAgainAfterResettingFilterTest() {
        String keyword = TeamCityDataGenerator.generateString("Fltr", 8);

        ProjectResponse matchingOne = UserSteps.createProjectWithName(keyword + "ProjectOne");
        ProjectResponse matchingTwo = UserSteps.createProjectWithName(keyword + "ProjectTwo");
        ProjectResponse unrelated =
                UserSteps.createProjectWithName(TeamCityDataGenerator.generateString("UnrelatedProject", 8));

        AdminProjectsPage adminProjectsPage = openAdminProjectsPage().filterByKeyword(keyword);
        Map<String, String> filteredProjects = adminProjectsPage.getDisplayedProjects();
        Map<String, String> projectsAfterReset = adminProjectsPage.resetFilter().getDisplayedProjects();

        attachProjects("UI projects filtered by '" + keyword + "'", filteredProjects);
        attachProjects("UI projects after filter reset", projectsAfterReset);

        softly.assertThat(filteredProjects)
                .as("Filter must hide the project that does not match the keyword")
                .containsExactlyInAnyOrderEntriesOf(asMap(matchingOne, matchingTwo));
        softly.assertThat(projectsAfterReset)
                .as("Resetting the filter must bring back every project, "
                        + "including the one the keyword filtered out")
                .containsAllEntriesOf(asMap(matchingOne, matchingTwo, unrelated));
    }

    private static AdminProjectsPage openAdminProjectsPage() {
        return new AdminProjectsPage().open()
                .checkItIsCorrectPage()
                .checkHeaderIsVisible()
                .checkFilterIsAvailable();
    }

    private static Map<String, String> asMap(ProjectResponse... projects) {
        return Arrays.stream(projects)
                .collect(Collectors.toMap(ProjectResponse::getId, ProjectResponse::getName));
    }

    static void attachProjects(String name, Map<String, String> projects) {
        String body = new TreeMap<>(projects).entrySet().stream()
                .map(project -> project.getKey() + " = " + project.getValue())
                .collect(Collectors.joining("\n"));
        Allure.addAttachment(name + " [" + projects.size() + "]", "text/plain", body, ".txt");
    }

    static void attachComparison(Map<String, String> uiProjects, Map<String, String> apiProjects) {
        Set<String> allIds = new TreeSet<>(uiProjects.keySet());
        allIds.addAll(apiProjects.keySet());

        String body = allIds.stream()
                .map(id -> "%s %-40s UI: %-35s API: %s".formatted(
                        Objects.equals(uiProjects.get(id), apiProjects.get(id)) ? "  " : "≠ ",
                        id,
                        uiProjects.getOrDefault(id, "<missing>"),
                        apiProjects.getOrDefault(id, "<missing>")))
                .collect(Collectors.joining("\n"));
        Allure.addAttachment("UI vs API comparison", "text/plain", body, ".txt");
    }
}
