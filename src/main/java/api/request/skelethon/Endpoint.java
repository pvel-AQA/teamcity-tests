package api.request.skelethon;

import api.models.*;
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

import static common.configs.Config.API_PREFIX;

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
    BUILD_QUEUE(
            "/buildQueue",
            BuildRunRequest.class,
            BuildRunResponse.class
    ),
    BUILD_QUEUE_ITEM(
            "/buildQueue/{queuedBuildLocato}",
            BaseModel.class,
            BuildRunResponse.class
    ),
    BUILD(
            "/builds/{buildLocator}",
            BuildRunRequest.class,        // Для обычного GET запроса тела нет
            BuildRunResponse.class   // GET возвращает стандартный объект сборки
    ),
    BUILD_CANCEL(
            "/builds/{buildLocator}",
            BuildCancelRequest.class, // POST принимает запрос на отмену
            BuildCancelResponse.class // POST возвращает статус отмены
    ),
    BUILD_QUEUE_PAUSED_STATE(
            "/buildQueue/pausedState",
            BuildQueuePausedRequest.class,
            Void.class
    );
    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;

    public boolean isDynamic() {
        return url.contains("{");
    }
}
