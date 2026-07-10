package api.project;

import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static api.errors.AuthErrorMessage.AUTHENTICATION_REQUIRED;
import static api.errors.ProjectErrors.NO_PROJECT_FOUND_BY_NAME_OR_INTERNAL_EXTERNAL_ID;
import static api.errors.ProjectErrors.PIPELINE_WITH_THIS_NAME_ALREADY_EXISTS;

public class ProjectTest extends BaseTest {

    @Test
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
    public void adminCannotDoCRUDOperationsWithoutAuthTest() {
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Project" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        int randomId = (int) (Math.random() * 100);
        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.PROJECTS,
                ResponseSpec.isUnauthorized(AUTHENTICATION_REQUIRED))
                .post(projectRequest);

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.PROJECTS,
                ResponseSpec.isUnauthorized(AUTHENTICATION_REQUIRED))
                .delete(randomId, null);

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.PROJECTS,
                ResponseSpec.isUnauthorized(AUTHENTICATION_REQUIRED))
                .get(projectRequest);

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.PROJECTS,
                ResponseSpec.isUnauthorized(AUTHENTICATION_REQUIRED))
                .put(randomId, projectRequest);
    }

    @Test
    public void adminCannotCreateProjectWithTheSameNameTest() {
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Project" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        ProjectResponse projectResponse = UserSteps.createProject(projectRequest);
        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.isBadRequest(PIPELINE_WITH_THIS_NAME_ALREADY_EXISTS.getErrorMsg()
                        + " " + projectRequest.getName()))
                .post(projectRequest);

        UserSteps.deleteProject(projectResponse);
    }

    @Test
    public void adminCanCreateProjectAndGetItByIdTest() {
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Project" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        ProjectResponse projectResponse = UserSteps.createProject(projectRequest);
        Assertions.assertThat(UserSteps.getProjectById(projectResponse.getId()).getName())
                .isEqualTo(projectRequest.getName());

        UserSteps.deleteProject(projectResponse);
    }

    @Test
    public void adminCannotGetNotExistingProjectByIdTest() {
        String projectToDelete = UUID.randomUUID().toString().substring(0, 8);
        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.notFound(NO_PROJECT_FOUND_BY_NAME_OR_INTERNAL_EXTERNAL_ID.getErrorMsg()
                        + " " + StringUtils.wrap(projectToDelete, "'"))
        ).get(projectToDelete);
    }

    @Test
    public void adminCannotDeleteNotExistingProjectByIdTest() {
        String projectToDelete = UUID.randomUUID().toString().substring(0, 8);
        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.notFound(NO_PROJECT_FOUND_BY_NAME_OR_INTERNAL_EXTERNAL_ID.getErrorMsg()
                        + " " + StringUtils.wrap(projectToDelete, "'"))
        ).delete(projectToDelete, null);
    }

    @Test
    public void adminCannotGetProjectWithWrongLocator() {
        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.isBadRequest()
        ).get("locator=count:" + UUID.randomUUID().toString().substring(0, 4));
    }
}
