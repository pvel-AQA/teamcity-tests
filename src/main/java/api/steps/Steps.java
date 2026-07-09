package api.steps;

import api.models.ProjectModel;
import api.models.projects.BuildTypeModel;
import api.models.projects.ProjectsResponse;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;

public class Steps {
    public static ProjectsResponse createProject(){
        ProjectModel createProjectRequest = ProjectModel.builder()
                .name("AutoProject11")
                .parentProjectName("id:_Root")
                .build();

        return new ValidatableCrudRequester<ProjectsResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.isOk())
                .post(createProjectRequest);
    }

    public static BuildTypeModel createConfig(String projectName){
        BuildTypeModel createConfigRequest = BuildTypeModel.builder()
                .name("AutoConfig_2")
                .projectId(projectName)
                .build();

        return new ValidatableCrudRequester<BuildTypeModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_TYPE,
                ResponseSpec.isOk())
                .post(createConfigRequest);
    }
}
