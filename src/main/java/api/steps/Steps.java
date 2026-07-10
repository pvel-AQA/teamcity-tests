package api.steps;

import api.models.ProjectModel;
import api.models.project.ProjectResponse;
import api.models.projects.BuildTypeModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;

public class Steps {
    public static ProjectResponse createProject(){
        ProjectModel createProjectRequest = ProjectModel.builder()
                .name("AutoProject14")
                .parentProjectName("id:_Root")
                .build();

        return new ValidatableCrudRequester<ProjectResponse>(
                RequestSpec.bearerSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.isOk())
                .post(createProjectRequest);
    }

    public static BuildTypeModel createConfig(String projectName){
        BuildTypeModel createConfigRequest = BuildTypeModel.builder()
                .name("AutoConfig")
                .projectId(projectName)
                .build();

        return new ValidatableCrudRequester<BuildTypeModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_TYPE,
                ResponseSpec.isOk())
                .post(createConfigRequest);
    }
}
