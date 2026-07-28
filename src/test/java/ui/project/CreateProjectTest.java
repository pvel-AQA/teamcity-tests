package ui.project;


import api.comparison.ModelAssertions;
import api.generators.RandomGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import com.codeborne.selenide.Condition;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ui.BaseUiTest;
import ui.elements.ProjectElement;
import ui.pages.ConnectVCSPage;
import ui.pages.CreateProjectPage;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ui.enums.errors.ProjectValidationError.*;


public class CreateProjectTest extends BaseUiTest {

    private final static String INVALID_ID_TO_BE_GENERATED = "id";
    private final static String INVALID_NAME_TO_BE_GENERATED = "name";

    private final static int EXPECTED_COUNT = 1;

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

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCannotCreateProjectWithNameAlreadyExistsTest() {
        var projectResponse = UserSteps.createProject();

        new CreateProjectPage()
                .open()
                .createProject(projectResponse.getName(), projectResponse.getId(), projectResponse.getDescription())
                .getProjectNameError().shouldBe(Condition.visible)
                .shouldHave(Condition.text(PROJECT_WITH_THIS_NAME_ALREADY_EXISTS.getErrorMsg()));

        var listOfProjects = UserSteps.getAllProjects().getProjects().stream()
                .filter(project -> project.getId().equals(projectResponse.getId())).toList();

        Assertions.assertThat(listOfProjects).hasSize(EXPECTED_COUNT);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCannotCreateProjectWithIDAlreadyExistsTest() {
        var projectResponse = UserSteps.createProject();

        new CreateProjectPage()
                .open()
                .createProject(TeamCityDataGenerator.generateString(), projectResponse.getId(), projectResponse.getDescription())
                .getProjectIdError().shouldBe(Condition.visible)
                .shouldHave(Condition.text(PROJECT_WITH_THIS_ID_ALREADY_EXIESTS.getErrorMsg()));

        var listOfProjects = UserSteps.getAllProjects().getProjects().stream()
                .filter(project -> project.getId().equals(projectResponse.getId())).toList();

        Assertions.assertThat(listOfProjects).hasSize(EXPECTED_COUNT);
    }


}
