package api.request.skelethon;

import api.models.BaseModel;
import api.models.BuildTypeStepsModel;
import api.models.ServerInfoResponse;
import api.models.projects.BuildTypeModel;
import api.models.projects.ProjectsResponse;
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
    private Class<? extends BaseModel> requestModel;
    private Class<? extends BaseModel> responseModel;
}
