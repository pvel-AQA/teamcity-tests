package ui.project;


import api.comparison.ModelAssertions;
import api.generators.RandomGenerator;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import com.codeborne.selenide.Condition;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;
import ui.BaseUiTest;
import ui.elements.ProjectElement;
import ui.pages.ConnectVCSPage;
import ui.pages.CreateProjectPage;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ui.enums.errors.ProjectValidationError.INVALID_PROJECT_ID;
import static ui.enums.errors.ProjectValidationError.PROJECT_NAME_CANNOT_BE_EMPTY;


public class CreateProjectTest extends BaseUiTest {

    private final static String INVALID_ID_TO_BE_GENERATED = "id";
    private final static String INVALID_NAME_TO_BE_GENERATED = "name";

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCanCreateProjectTest() {
        var projectRequest = RandomGenerator.generate(ProjectRequest.class);

        ProjectElement uiProject = new CreateProjectPage()
                .open()
                .createProject(projectRequest.getName(), projectRequest.getId(), projectRequest.getDescription())
                .getPage(ConnectVCSPage.class)
                .clickProceedWithoutRepositoryButton()
                .clickSkipButton()
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
    public void userCannotCreateProjectWithInvalidIdTest() {
        var projectRequest = RandomGenerator.generate(ProjectRequest.class, INVALID_ID_TO_BE_GENERATED);

        new CreateProjectPage()
                .open()
                .createProject(projectRequest.getName(), projectRequest.getId(), projectRequest.getDescription())
                .getProjectIdError().shouldBe(Condition.visible)
                .shouldHave(Condition.text(INVALID_PROJECT_ID.getErrorMsg()));

        boolean projectExists = UserSteps.getAllProjects().getProjects().stream()
                .anyMatch(project -> project.getId().equals(projectRequest.getId()));

        assertThat(projectExists).isFalse();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCannotCreateProjectWithEmptyNameTest() {
        var projectRequest = RandomGenerator.generate(ProjectRequest.class, INVALID_NAME_TO_BE_GENERATED);

        new CreateProjectPage()
                .open()
                .createProject(projectRequest.getName(), projectRequest.getId(), projectRequest.getDescription())
                .getProjectNameError().shouldBe(Condition.visible)
                .shouldHave(Condition.text(PROJECT_NAME_CANNOT_BE_EMPTY.getErrorMsg()));

        boolean projectExists = UserSteps.getAllProjects().getProjects().stream()
                .anyMatch(project -> project.getId().equals(projectRequest.getId()));

        assertThat(projectExists).isFalse();
    }


}
