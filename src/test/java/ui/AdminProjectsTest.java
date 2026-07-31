package ui;

import api.generators.RandomGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.project.AllProjectsResponse;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
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
    void adminFiltersProjectsByKeywordOnAdminPageTest() {
        String keyword = TeamCityDataGenerator.generateString("Fltr", 8);
        String unrelatedSuffix = TeamCityDataGenerator.generateString("Other", 8);

        ProjectResponse keywordAtStart = UserSteps.createProjectWithName(keyword + "AtStartProject");
        ProjectResponse keywordInMiddle = UserSteps.createProjectWithName("Project" + keyword + "InMiddle");
        ProjectResponse keywordAtEnd = UserSteps.createProjectWithName("ProjectEndsWith" + keyword);

        ProjectResponse parentOfMatchingSubOne =
                UserSteps.createProjectWithName("ParentOfMatchingSubOne" + unrelatedSuffix);
        ProjectResponse matchingSubOne =
                UserSteps.createSubProject(parentOfMatchingSubOne.getId(), "Sub" + keyword + "One");
        ProjectResponse parentOfMatchingSubTwo =
                UserSteps.createProjectWithName("ParentOfMatchingSubTwo" + unrelatedSuffix);
        ProjectResponse matchingSubTwo =
                UserSteps.createSubProject(parentOfMatchingSubTwo.getId(), "Sub" + keyword + "Two");

        ProjectResponse parentOfNonMatchingSubOne =
                UserSteps.createProjectWithName("ParentOfNonMatchingSubOne" + unrelatedSuffix);
        ProjectResponse nonMatchingSubOne =
                UserSteps.createSubProject(parentOfNonMatchingSubOne.getId(), "PlainSubOne" + unrelatedSuffix);
        ProjectResponse parentOfNonMatchingSubTwo =
                UserSteps.createProjectWithName("ParentOfNonMatchingSubTwo" + unrelatedSuffix);
        ProjectResponse nonMatchingSubTwo =
                UserSteps.createSubProject(parentOfNonMatchingSubTwo.getId(), "PlainSubTwo" + unrelatedSuffix);

        ProjectResponse archivedOne = UserSteps.createArchivedProjectWithName("Archived" + keyword + "One");
        ProjectResponse archivedTwo = UserSteps.createArchivedProjectWithName("Archived" + keyword + "Two");

        ProjectResponse unrelatedOne = UserSteps.createProjectWithName("UnrelatedProjectOne" + unrelatedSuffix);
        ProjectResponse unrelatedTwo = UserSteps.createProjectWithName("UnrelatedProjectTwo" + unrelatedSuffix);

        Map<String, String> expectedActiveMatches = asMap(
                keywordAtStart, keywordInMiddle, keywordAtEnd,
                parentOfMatchingSubOne, matchingSubOne,
                parentOfMatchingSubTwo, matchingSubTwo);
        Map<String, String> expectedArchivedMatches = asMap(archivedOne, archivedTwo);
        Map<String, String> expectedNonMatches = asMap(
                parentOfNonMatchingSubOne, nonMatchingSubOne,
                parentOfNonMatchingSubTwo, nonMatchingSubTwo,
                unrelatedOne, unrelatedTwo);

        AdminProjectsPage adminProjectsPage = new AdminProjectsPage().open()
                .checkItIsCorrectPage()
                .checkHeaderIsVisible()
                .showArchivedProjects()
                .doNotShowArchivedProjects()
                .checkFilterIsAvailable()
                .filterByKeyword(keyword);

        Map<String, String> activeMatches = adminProjectsPage.getDisplayedProjects();
        attachProjects("UI projects filtered by '" + keyword + "'", activeMatches);
        attachProjects("Expected active matches", expectedActiveMatches);
        attachProjects("Expected archived matches (hidden until 'Show archived')", expectedArchivedMatches);
        attachProjects("Expected non-matches", expectedNonMatches);

        softly.assertThat(adminProjectsPage.getFilterKeyword())
                .as("Keyword field must keep the applied filter")
                .isEqualTo(keyword);
        softly.assertThat(activeMatches)
                .as("Filter must show every active project matching the keyword by name, "
                        + "including the parents of the matching sub-projects, and nothing else")
                .containsExactlyInAnyOrderEntriesOf(expectedActiveMatches);
        softly.assertThat(activeMatches.keySet())
                .as("Archived projects must stay hidden while 'Show archived' is off")
                .doesNotContainAnyElementsOf(expectedArchivedMatches.keySet());
        softly.assertThat(activeMatches.keySet())
                .as("Projects that do not match the keyword must be filtered out")
                .doesNotContainAnyElementsOf(expectedNonMatches.keySet());

        adminProjectsPage
                .checkProjectIsVisible(matchingSubOne.getId(), matchingSubOne.getName())
                .checkProjectIsVisible(matchingSubTwo.getId(), matchingSubTwo.getName());
        softly.assertThat(adminProjectsPage.getProjectDepth(matchingSubOne.getId()))
                .as("A matching sub-project must stay nested under its parent in the filtered tree")
                .isEqualTo(adminProjectsPage.getProjectDepth(parentOfMatchingSubOne.getId()) + 1);

        adminProjectsPage
                .filterByKeyword(TeamCityDataGenerator.generateString("NoSuchProject", 8))
                .checkNothingMatchesTheFilter();

        Map<String, String> projectsAfterReset = adminProjectsPage
                .resetFilter()
                .getDisplayedProjects();
        attachProjects("UI projects after filter reset", projectsAfterReset);

        softly.assertThat(projectsAfterReset)
                .as("Resetting the filter must bring every active top level project back")
                .containsAllEntriesOf(merge(
                        asMap(keywordAtStart, keywordInMiddle, keywordAtEnd),
                        asMap(parentOfMatchingSubOne, parentOfMatchingSubTwo,
                                parentOfNonMatchingSubOne, parentOfNonMatchingSubTwo,
                                unrelatedOne, unrelatedTwo)));
        softly.assertThat(projectsAfterReset.keySet())
                .as("Resetting the filter must not reveal archived projects")
                .doesNotContainAnyElementsOf(expectedArchivedMatches.keySet());

        Map<String, String> matchesWithArchived = adminProjectsPage
                .filterByKeyword(keyword)
                .showArchivedProjects()
                .getDisplayedProjects();
        attachProjects("UI projects filtered by '" + keyword + "' with archived shown", matchesWithArchived);

        softly.assertThat(adminProjectsPage.getFilterKeyword())
                .as("Keyword field must keep the applied filter after enabling 'Show archived'")
                .isEqualTo(keyword);
        softly.assertThat(matchesWithArchived)
                .as("'Show archived' must add the matching archived projects to the filtered result")
                .containsExactlyInAnyOrderEntriesOf(merge(expectedActiveMatches, expectedArchivedMatches));
        expectedArchivedMatches.keySet().forEach(projectId ->
                softly.assertThat(adminProjectsPage.isProjectMarkedArchived(projectId))
                        .as("Project %s must be marked as archived", projectId)
                        .isTrue());
    }

    private static Map<String, String> asMap(ProjectResponse... projects) {
        return Arrays.stream(projects)
                .collect(Collectors.toMap(ProjectResponse::getId, ProjectResponse::getName));
    }

    private static Map<String, String> merge(Map<String, String> first, Map<String, String> second) {
        Map<String, String> merged = new HashMap<>(first);
        merged.putAll(second);
        return merged;
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
