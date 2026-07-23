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
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static api.enums.errors.AuthErrorMessage.AUTHENTICATION_REQUIRED;
import static api.enums.errors.ProjectErrors.NO_PROJECT_FOUND_BY_NAME_OR_INTERNAL_EXTERNAL_ID;

public class GetProjectTest extends BaseTest {

    @Test
    public void adminCannotDoCRUDOperationsWithoutAuthTest() {
        ProjectRequest projectRequest = RandomGenerator.generate(ProjectRequest.class);
        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .get(projectRequest);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCannotGetNotExistingProjectByIdTest() {
        String projectToDelete = UUID.randomUUID().toString().substring(0, 8);
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsNotFound(NO_PROJECT_FOUND_BY_NAME_OR_INTERNAL_EXTERNAL_ID.getErrorMsg()
                        + " " + StringUtils.wrap(projectToDelete, "'"))
        ).get(projectToDelete);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCannotGetProjectWithWrongLocator() {
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsBadRequest()
        ).get("locator=count:" + UUID.randomUUID().toString().substring(0, 4));
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanCreateProjectAndGetItByIdTest() {
        ProjectRequest projectRequest = RandomGenerator.generate(ProjectRequest.class);
        ProjectResponse projectResponse = UserSteps.createProjectWithExtension(projectRequest);
        Assertions.assertThat(UserSteps.getProjectById(projectResponse.getId()).getName())
                .isEqualTo(projectRequest.getName());
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanGetProjectByNameTest() {
        ProjectRequest projectRequest = RandomGenerator.generate(ProjectRequest.class);
        ProjectResponse projectResponse = UserSteps.createProjectWithExtension(projectRequest);

        ProjectResponse projectByName = UserSteps.getProjectById(projectRequest.getName());
        Assertions.assertThat(projectByName.getId()).isEqualTo(projectResponse.getId());
        Assertions.assertThat(projectByName.getName()).isEqualTo(projectRequest.getName());
    }

}
