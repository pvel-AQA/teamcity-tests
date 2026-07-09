package api.request.skelethon;

import api.models.BaseModel;
import api.models.ServerInfoResponse;
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
    PROJECTS2(
            "/projects",
            BaseModel.class,
            ProjectsResponse.class
    ),
    BUILD_TYPE(
            "/buildTypes",
            BaseModel.class,
            BuildTypeModel.class
    ),
    BUILD_STEP(
            "/buildTypes/{btLocator}/steps",
            BaseModel.class,
            BuildTypeStepsModel.class
    );
    /*SERVER(
            "/server",
            BaseModel.class,
            ServerInfoResponse.class
    ),
    SERVER(
            "/server",
            BaseModel.class,
            ServerInfoResponse.class
    ),
    SERVER(
            "/server",
            BaseModel.class,
            ServerInfoResponse.class
    );*/
    private String url;
    private Class<?> requestModel;
    private Class<?> responseModel;
}
