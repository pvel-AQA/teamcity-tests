package api.steps;

import api.models.project.AllProjectsResponse;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import common.configs.Config;

import static common.configs.Config.ADMIN_PASSWORD;
import static common.configs.Config.ADMIN_USERNAME;

public class UserSteps {

    public static ProjectResponse createProject(ProjectRequest projectRequest) {
        return new ValidatableCrudRequester<ProjectResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.isOk()
        ).post(projectRequest);
    }

    public static ProjectResponse createProject(String username, String password, ProjectRequest projectRequest) {
        return new ValidatableCrudRequester<ProjectResponse>(
                RequestSpec.basicAuthSpec(username, password),
                Endpoint.PROJECTS,
                ResponseSpec.isOk()
        ).post(projectRequest);
    }

    public static AllProjectsResponse getAllProjects() {
        return new ValidatableCrudRequester<AllProjectsResponse>(
                RequestSpec.authAsUserSpec(Config.getProperty(ADMIN_USERNAME), Config.getProperty(ADMIN_PASSWORD)),
                Endpoint.ALL_PROJECTS,
                ResponseSpec.isOk()
        ).get(null);
    }

    public static ProjectResponse getProjectById(String id) {
        return new ValidatableCrudRequester<ProjectResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.isOk()
        ).get(id);
    }

    public static boolean isProjectExists(String projectName) {
        return UserSteps.getAllProjects().getProjects().stream()
                .anyMatch(project -> project.getName().equals(projectName));
    }

    public static void deleteProject(String username, String password, ProjectResponse projectResponse) {
        new ValidatableCrudRequester<ProjectResponse>(
                RequestSpec.authAsUserSpec(username, password),
                Endpoint.PROJECTS,
                ResponseSpec.deleted()
        ).delete(projectResponse.getId(), null);
    }

    public static void deleteProject(ProjectResponse projectResponse) {
        deleteProject(Config.getProperty(ADMIN_USERNAME), Config.getProperty(ADMIN_PASSWORD), projectResponse);
    }

}
