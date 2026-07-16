package api.steps;

import api.enums.locators.LocatorType;
import api.generators.RandomGenerator;
import api.models.UserTokenRequest;
import api.models.UserTokenResponse;
import api.models.agent.Agent;
import api.models.agent.GetAgentsResponse;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildConfigurationResponse;
import api.models.project.AllProjectsResponse;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.models.user.UserRequest;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
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
        return new ValidatedCrudRequester<ProjectResponse>(
                RequestSpec.authAsUserSpec(username, password),
                Endpoint.PROJECTS,
                ResponseSpec.returnsOk()
        ).post(projectRequest);
    }

    public static ProjectResponse createProjectWithExtension(ProjectRequest projectRequest) {
        return new ValidatedCrudRequester<ProjectResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsOk()
        ).post(projectRequest);
    }

    public static AllProjectsResponse getAllProjects() {
        return new ValidatedCrudRequester<AllProjectsResponse>(
                RequestSpec.authAsUserSpec(ADMIN_USERNAME, ADMIN_PASSWORD),
                Endpoint.ALL_PROJECTS,
                ResponseSpec.returnsOk()
        ).get();
    }

    public static ProjectResponse getProjectById(String id) {
        return new ValidatedCrudRequester<ProjectResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.returnsOk()
        ).get(id);
    }

    public static boolean isProjectExists(String projectName) {
        return UserSteps.getAllProjects().getProjects().stream()
                .anyMatch(project -> project.getName().equals(projectName));
    }

    public static void deleteProject(String username, String password, ProjectResponse projectResponse) {
        new CrudRequester(
                RequestSpec.authAsUserSpec(username, password),
                Endpoint.PROJECTS,
                ResponseSpec.returnsDeleted()
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
        return new ValidatedCrudRequester<BuildConfigurationResponse>(RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_TYPES,
                ResponseSpec.returnsOk())
                .post(buildConf);
    }

    public static BuildConfigurationResponse getBuilds() {
        return new ValidatedCrudRequester<BuildConfigurationResponse>(RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_TYPES,
                ResponseSpec.returnsOk())
                .get();
    }

    public static UserTokenResponse getUserToken(UserRequest userRequest) {
        return new ValidatedCrudRequester<UserTokenResponse>(
                RequestSpec.authAsUserSpec(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.USER_TOKEN,
                ResponseSpec.returnsOk())
                .post(UserTokenRequest.builder().name(userRequest.getUsername()).build(),
                        LocatorType.ID + userRequest.getId());
    }

    public static int getAgentId() {
        return new ValidatedCrudRequester<GetAgentsResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS,
                ResponseSpec.returnsOk()
        ).get(new CrudRequester.QueryBuilder()
                        .locatorEqualsAuthorizedAny()
                        .locatorEqualsConnectedTrue().build())
                .getAgent().getFirst().getId();
    }

    public static boolean getAgentAuthorizedStatus(int agentId) {
        return new ValidatedCrudRequester<Agent>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.AGENTS_WITH_LOCATOR,
                ResponseSpec.returnsOk()
        ).get(LocatorType.ID.getPrefix() + agentId)
                .getAuthorizedInfo().isStatus();
    }

}
