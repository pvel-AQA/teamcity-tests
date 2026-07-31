package ui;

import api.models.project.AllProjectsResponse;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import ui.base.BaseUiTest;
import ui.pages.AdminProjectsPage;

import java.util.Map;
import java.util.stream.Collectors;

import static ui.AdminProjectsTest.attachComparison;
import static ui.AdminProjectsTest.attachProjects;

public class AdminIsolatedTest extends BaseUiTest {
    @Test
    @ResourceLock(value = Resources.GLOBAL, mode = ResourceAccessMode.READ_WRITE)
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminSeesExactlyTheSameProjectsOnAdminPageAsApiReturnsTest() {
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
        softly.assertThat(displayedCount).isEqualTo(apiProjects.size());
        softly.assertThat(descriptionCount-1).isEqualTo(apiProjects.size());
    }
}
