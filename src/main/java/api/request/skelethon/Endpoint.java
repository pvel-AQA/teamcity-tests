package api.request.skelethon;

import api.models.BaseModel;
import api.models.ServerInfoResponse;
import api.models.UserTokenRequest;
import api.models.UserTokenResponse;
import api.models.agent.Agent;
import api.models.agent.AuthorizeAgentRequest;
import api.models.agent.AuthorizeAgentResponse;
import api.models.agent.GetAgentsResponse;
import api.models.build.*;
import api.models.project.AllProjectsResponse;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.models.user.UserRequest;
import api.models.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum Endpoint {
    SERVER(
            "/server",
            BaseModel.class,
            ServerInfoResponse.class
    ),
    PROJECTS(
            "/projects",
            ProjectRequest.class,
            ProjectResponse.class
    ),
    ALL_PROJECTS(
            "/projects",
            BaseModel.class,
            AllProjectsResponse.class
    ),
    USERS(
            "/users",
            UserRequest.class,
            UserResponse.class
    ),
    BUILD_TYPES(
            "/buildTypes",
            BuildConfigurationRequest.class,
            BuildConfigurationResponse.class
    ),
    BUILD_TYPE(
            "/buildTypes/{buildTypeLocator}",
            BuildConfigurationRequest.class,
            BuildConfigurationResponse.class
    ),
    USER_TOKEN(
            "/users/{userLocator}/tokens",
            UserTokenRequest.class,
            UserTokenResponse.class
    ),
    PROJECTS_BUILD_TYPES(
            "/projects/{projectId}/buildTypes",
            BaseModel.class,
            BuildConfigurationResponse.class
    ),
    AGENTS(
            "/agents",
            BaseModel.class,
            GetAgentsResponse.class
    ),
    AGENTS_WITH_LOCATOR(
            "/agents/{locator}",
            BaseModel.class,
            Agent.class
    ),
    AGENTS_AUTHORIZED_INFO(
            "/agents/{agentLocator}/authorizedInfo",
            AuthorizeAgentRequest.class,
            AuthorizeAgentResponse.class
    ),
    BUILD_STEP_CREATE(
            "/buildTypes/{btLocator}/steps",
            BaseModel.class,
            BuildTypeStepsModel.class
    ),
    BUILD_STEPS_READ(
            "/buildTypes/{btLocator}/steps",
            BaseModel.class,
            BuildTypeStepsList.class
    ),
    BUILD_STEP_READ(
            "/buildTypes/{btLocator}/steps/{stepId}",
            BaseModel.class,
            BuildTypeStepsModel.class
    ),
    BUILD_STEP_UPDATE(
            "/buildTypes/{btLocator}/steps/{stepId}",
            BuildTypeStepsModel.class,
            BuildTypeStepsModel.class
    ),
    BUILD_STEP_DELETE(
            "/buildTypes/{btLocator}/steps/{stepId}",
            BaseModel.class,
            BaseModel.class
    ),
    BUILD_QUEUE(
            "/buildQueue",
            BuildRequest.class,
            Build.class
    ),
    BUILD(
            "/builds/{buildLocator}",
            BaseModel.class,
            Build.class
    )
    ;

    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;

    public boolean isDynamic() {
        return url.contains("{");
    }
}
