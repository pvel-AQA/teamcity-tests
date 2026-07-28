package ui;

import api.models.project.AllProjectsResponse;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Test;
import ui.pages.AdminProjectsPage;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AdminProjectsTest extends BaseUiTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminCanOpenAdminProjectsPageAndSeeTopProjectsInfoTest() {

        //UserSteps.createProject();
        AdminProjectsPage adminProjectsPage = new AdminProjectsPage().open()
                .checkItIsCorrectPage()
                .checkHeaderIsVisible();

        Map<String, String> uiProjects = adminProjectsPage.getDisplayedProjects();
        int displayedCount = adminProjectsPage.getDisplayedProjectsCount();
        int descriptionCount = adminProjectsPage.getActiveProjectsCountFromDescription();

        AllProjectsResponse allProjects = UserSteps.getAllProjects();
        Map<String, String> apiProjects = allProjects.getProjects().stream()
                .filter(project -> !AdminProjectsPage.ROOT_PROJECT_ID.equals(project.getId()))
                .collect(Collectors.toMap(ProjectResponse::getId, ProjectResponse::getName));

        attachProjects("UI projects (Admin page)", uiProjects);
        attachProjects("API projects", apiProjects);
        attachComparison(uiProjects, apiProjects);

        softly.assertThat(uiProjects).containsExactlyInAnyOrderEntriesOf(apiProjects);
        softly.assertThat(displayedCount).isEqualTo(allProjects.getCount());
        softly.assertThat(descriptionCount-1).isEqualTo(allProjects.getCount());
    }

    private static void attachProjects(String name, Map<String, String> projects) {
        String body = new TreeMap<>(projects).entrySet().stream()
                .map(project -> project.getKey() + " = " + project.getValue())
                .collect(Collectors.joining("\n"));
        Allure.addAttachment(name + " [" + projects.size() + "]", "text/plain", body, ".txt");
    }

    private static void attachComparison(Map<String, String> uiProjects, Map<String, String> apiProjects) {
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
