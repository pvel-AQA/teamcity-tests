package api.request.skelethon;

import api.models.BaseModel;
import api.models.BuildTypeStepsModel;
import api.models.ServerInfoResponse;
import api.models.project.AllProjectsResponse;
import api.models.project.ProjectRequest;
import api.models.project.ProjectResponse;
import api.models.projects.BuildTypeModel;
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
    PROJECTS("projects",
            ProjectRequest.class,
            ProjectResponse.class
    ),
    ALL_PROJECTS("projects",
            null,
            AllProjectsResponse.class
    ),
    USERS("users",
            UserRequest.class,
            UserResponse.class
    ),
    BUILD_TYPE(
            "/buildTypes",
            BaseModel.class,
            BuildTypeModel.class
    ),
    BUILD_STEP_CREATE(
            "/buildTypes/{btLocator}/steps",
            BuildTypeStepsModel.class,
            BuildTypeStepsModel.class
    ),
    BUILD_STEP_RUD(
            "/buildTypes/{btLocator}/steps/{stepId}",
            BuildTypeStepsModel.class,
            BuildTypeStepsModel.class
    );

    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;
}
