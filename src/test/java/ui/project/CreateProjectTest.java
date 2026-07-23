package ui.project;


import api.comparison.ModelAssertions;
import api.generators.RandomGenerator;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;
import ui.BaseUiTest;
import ui.elements.ProjectElement;
import ui.enums.errors.ProjectValidationError;
import ui.pages.CreateProjectPage;
import ui.pages.ProjectsPage;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class CreateProjectTest extends BaseUiTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCanCreateProjectTest() {
        var projectRequest = RandomGenerator.generate(ProjectRequest.class);

        ProjectElement uiProject = new CreateProjectPage()
                .open()
                .createProject(projectRequest.getName(), projectRequest.getId(), projectRequest.getDescription())
                .getPage(ProjectsPage.class)
                .open()
                .getProjects()
                .stream()
                .filter(p -> p.getProjectName().equals(projectRequest.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("There is no project with name: " + projectRequest.getName()));

        ProjectResponse apiProject = UserSteps.getProjectById(projectRequest.getId());

        softly.assertThat(apiProject.getName()).isEqualTo(uiProject.getProjectName());

        ModelAssertions.assertThatModels(projectRequest, apiProject);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCantCreateProjectWithInvalidIdTest() {
        var projectRequest = RandomGenerator.generate(ProjectRequest.class, "id");

        assertThat(new CreateProjectPage()
                .open()
                .createProject(projectRequest.getName(), projectRequest.getId(), projectRequest.getDescription())
                .checkProjectIdError(ProjectValidationError.INVALID_PROJECT_ID)
                .getPage(ProjectsPage.class)
                .open()
                .getProjects().stream()
                .noneMatch(p -> p.getProjectName().equals(projectRequest.getName())))
                .isTrue();

        long foundProject = UserSteps.getAllProjects().getProjects().stream()
                .filter(project -> project.getId().equals(projectRequest.getId())).count();

        assertThat(foundProject).isZero();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCantCreateProjectWithEmptyNameTest() {
        var projectRequest = RandomGenerator.generate(ProjectRequest.class, "name");

        assertThat(new CreateProjectPage()
                .open()
                .createProject(projectRequest.getName(), projectRequest.getId(), projectRequest.getDescription())
                .checkProjectNameError(ProjectValidationError.PROJECT_NAME_CANNOT_BE_EMPTY)
                .getPage(ProjectsPage.class)
                .open()
                .getProjects().stream()
                .noneMatch(p -> p.getProjectName().equals(projectRequest.getName())))
                .isTrue();

        long foundProject = UserSteps.getAllProjects().getProjects().stream()
                .filter(project -> project.getId().equals(projectRequest.getId())).count();

        assertThat(foundProject).isZero();
    }


}
