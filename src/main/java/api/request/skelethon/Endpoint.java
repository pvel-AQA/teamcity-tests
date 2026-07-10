package api.request.skelethon;

import api.models.*;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildConfigurationResponse;
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
            API_PREFIX + "/server",
            BaseModel.class,
            ServerInfoResponse.class
    ),
    PROJECTS(API_PREFIX + "/projects",
            ProjectRequest.class,
            ProjectResponse.class
    ),
    ALL_PROJECTS(API_PREFIX + "/projects",
            BaseModel.class,
            AllProjectsResponse.class
    ),
    USERS(API_PREFIX + "/users",
            UserRequest.class,
            UserResponse.class
    ),
    BUILD_TYPES(
            API_PREFIX + "/buildTypes",
            BuildConfigurationRequest.class,
            BuildConfigurationResponse.class
    ),
    PROJECTS_BUILD_TYPES(
            API_PREFIX + "/projects/id:{projectId}/buildTypes",
            BaseModel.class,
            BuildConfigurationResponse.class
    );
    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;

    public boolean isDynamic() {
        return url.contains("{");
    }
}
