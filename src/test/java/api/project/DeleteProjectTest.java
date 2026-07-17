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

public class DeleteProjectTest extends BaseTest {

    @Test
    public void adminCannotDeleteWithoutAuthTest() {
        int randomId = (int) (Math.random() * 100);
        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .delete(randomId, null);

    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCannotDeleteNotExistingProjectByIdTest() {
        String notExistingProjectId = UUID.randomUUID().toString().substring(0, 8);
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsNotFound(NO_PROJECT_FOUND_BY_NAME_OR_INTERNAL_EXTERNAL_ID.getErrorMsg()
                        + " " + StringUtils.wrap(notExistingProjectId, "'"))
        ).delete(notExistingProjectId, null);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCannotDeleteProjectWithEmptyIdTest() {
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsNotFound()
        ).delete("", null);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCannotDeleteProjectWithBothIdAndLocatorNullTest() {
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsBadRequest()
        ).delete(null, null);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void adminCanDeleteProjectTest() {
        ProjectRequest projectRequest = RandomGenerator.generate(ProjectRequest.class);
        ProjectResponse projectResponse = UserSteps.createProject(projectRequest);
        UserSteps.deleteProject(projectResponse);

        boolean projectExists = UserSteps.isProjectExists(projectRequest.getName());
        Assertions.assertThat(projectExists).isFalse()
                .as("Deleted project shouldn't exist");
    }

}
