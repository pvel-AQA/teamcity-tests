package api.project;

import api.models.project.AllProjectsResponse;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.models.user.UserResponse;
import api.steps.SuperUserSteps;
import api.steps.UserSteps;
import base.BaseTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class ProjectTest extends BaseTest {

    @Test
    public void adminCanCreateProjectOnlyWithMandatoryParams() {
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Project" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        ProjectResponse projectResponse = UserSteps.createProject(projectRequest);
        Assertions.assertThat(projectRequest.getName()).isEqualTo(projectResponse.getName());

        AllProjectsResponse list = UserSteps.getProjects();

        UserSteps.deleteProject(projectResponse);
    }

}
