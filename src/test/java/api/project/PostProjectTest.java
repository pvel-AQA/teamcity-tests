package api.project;

import api.generators.RandomGenerator;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static api.enums.errors.AuthErrorMessage.AUTHENTICATION_REQUIRED;
import static api.enums.errors.ProjectErrors.PIPELINE_WITH_THIS_NAME_ALREADY_EXISTS;

public class PostProjectTest extends BaseTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanCreateProjectOnlyWithMandatoryParamsTest() {
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Project" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        ProjectResponse projectResponse = UserSteps.createProject(projectRequest);
        Assertions.assertThat(projectRequest.getName()).isEqualTo(projectResponse.getName());
        boolean projectExists = UserSteps.isProjectExists(projectRequest.getName());
        Assertions.assertThat(projectExists).isTrue()
                .as("Created project should exists, trying to find it with getAll");

        UserSteps.deleteProject(projectResponse);
        projectExists = UserSteps.isProjectExists(projectRequest.getName());
        Assertions.assertThat(projectExists).isFalse()
                .as("Deleted project shouldn't exist");
    }

    @Test
    public void adminCannotCreateProjectWithoutAuthTest() {
        ProjectRequest projectRequest = RandomGenerator.generate(ProjectRequest.class);
        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .post(projectRequest);

    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCannotCreateProjectWithTheSameNameTest() {
        ProjectRequest projectRequest = RandomGenerator.generate(ProjectRequest.class);
        ProjectResponse projectResponse = UserSteps.createProjectWithExtension(projectRequest);
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsBadRequest(PIPELINE_WITH_THIS_NAME_ALREADY_EXISTS.getErrorMsg()
                        + " " + projectRequest.getName()))
                .post(projectRequest);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanCreateProjectWithAllParamsTest() {
        ProjectRequest projectRequest = RandomGenerator.generate(ProjectRequest.class);
        ProjectResponse projectResponse = UserSteps.createProject(projectRequest);
        Assertions.assertThat(projectRequest.getName()).isEqualTo(projectResponse.getName());
        boolean projectExists = UserSteps.isProjectExists(projectRequest.getName());
        Assertions.assertThat(projectExists).isTrue()
                .as("Created project should exists, trying to find it with getAll");
    }

}
