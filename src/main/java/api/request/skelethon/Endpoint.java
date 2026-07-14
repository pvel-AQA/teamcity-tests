package api.request.skelethon;

import api.models.*;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildConfigurationResponse;
import api.models.BaseModel;
import api.models.BuildTypeStepsModel;
import api.models.ServerInfoResponse;
import api.models.project.AllProjectsResponse;
import api.models.user.UserRequest;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.models.user.UserRequest;
import api.models.projects.BuildTypeModel;
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
    PROJECTS(
            API_PREFIX + "/projects",
            ProjectRequest.class,
            ProjectResponse.class
    ),
    ALL_PROJECTS(
            API_PREFIX + "/projects",
            null,
            AllProjectsResponse.class
    ),
    USERS(
            API_PREFIX + "/users",
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
    ),
    BUILD_TYPE(
            API_PREFIX + "/buildTypes",
            BaseModel.class,
            BuildTypeModel.class
    ),
    BUILD_STEP_CREATE(
            API_PREFIX + "/buildTypes/{btLocator}/steps",
            BuildTypeStepsModel.class,
            BuildTypeStepsModel.class
    ),
    BUILD_STEP_RUD(
            API_PREFIX + "/buildTypes/{btLocator}/steps/{stepId}",
            BuildTypeStepsModel.class,
            BuildTypeStepsModel.class
    );

    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;

    public boolean isDynamic() {
        return url.contains("{");
    }
}
