package api.steps;

import api.generators.RandomGenerator;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildConfigurationResponse;
import api.models.project.AllProjectsResponse;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;

import static common.configs.Config.*;

public class UserSteps {

    public static ProjectResponse createProject() {
        ProjectRequest projectRequest = ProjectRequest.builder()
                .name("Project" + RandomGenerator.generateString())
                .build();
        return createProject(projectRequest);
    }

    public static ProjectResponse createProject(ProjectRequest projectRequest) {
        return createProject(ADMIN_USERNAME, ADMIN_PASSWORD, projectRequest);
    }

    public static ProjectResponse createProject(String username, String password, ProjectRequest projectRequest) {
        return new ValidatableCrudRequester<ProjectResponse>(
                RequestSpec.authAsUserSpec(username, password),
                Endpoint.PROJECTS,
                ResponseSpec.isOk()
        ).post(projectRequest);
    }

    public static AllProjectsResponse getProjects() {
        return new ValidatableCrudRequester<AllProjectsResponse>(
                RequestSpec.authAsUserSpec(ADMIN_USERNAME, ADMIN_PASSWORD),
                Endpoint.ALL_PROJECTS,
                ResponseSpec.isOk()
        ).get();
    }

    public static void deleteProject(String username, String password, ProjectResponse projectResponse) {
        new CrudRequester(
                RequestSpec.authAsUserSpec(username, password),
                Endpoint.PROJECTS,
                ResponseSpec.deleted()
        ).delete(projectResponse.getId());
    }

    public static void deleteProject(ProjectResponse projectResponse) {
         deleteProject(ADMIN_USERNAME, ADMIN_PASSWORD, projectResponse);
    }

    public static BuildConfigurationResponse createBuildConfiguration() {
        ProjectResponse project = createProject();
        BuildConfigurationRequest buildRequest = RandomGenerator.generate(BuildConfigurationRequest.class);
        buildRequest.getProject().setId(project.getId());

        return createBuildConfiguration(buildRequest);
    }

    public static BuildConfigurationResponse createBuildConfiguration(BuildConfigurationRequest buildConf) {
        return new ValidatableCrudRequester<BuildConfigurationResponse>(RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_TYPES,
                ResponseSpec.isOk())
                .post(buildConf);
    }

    public static BuildConfigurationResponse getBuilds() {
        return new ValidatableCrudRequester<BuildConfigurationResponse>(RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_TYPES,
                ResponseSpec.isOk())
                .get();
    }

}
