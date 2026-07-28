package ui;

import api.models.project.AllProjectsResponse;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;
import ui.pages.AdminProjectsPage;

import java.util.Map;
import java.util.stream.Collectors;

public class AdminProjectsTest extends BaseUiTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    void adminCanOpenAdminProjectsPageAndSeeTopProjectsInfoTest() {
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

        softly.assertThat(uiProjects).containsExactlyInAnyOrderEntriesOf(apiProjects);
        softly.assertThat(displayedCount).isEqualTo(allProjects.getCount());
        softly.assertThat(descriptionCount).isEqualTo(allProjects.getCount());
    }
}
