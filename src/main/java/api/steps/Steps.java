package api.steps;

import api.generators.RandomDataGenerator;
import api.models.BuildTypeStepsModel;
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
                .name(RandomDataGenerator.randomSpecificString("AutoProject",5))
                .parentProjectName("id:_Root")
                .build();

        return new ValidatableCrudRequester<ProjectResponse>(
                RequestSpec.basicAuthSpec(),
                Endpoint.PROJECTS,
                ResponseSpec.isOk())
                .post(createProjectRequest);
    }

    public static BuildTypeModel createConfig(String projectName){
        BuildTypeModel createConfigRequest = BuildTypeModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoConfig",5))
                .projectId(projectName)
                .build();

        return new ValidatableCrudRequester<BuildTypeModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_TYPE,
                ResponseSpec.isOk())
                .post(createConfigRequest);
    }

    public static BuildTypeStepsModel getBuildTypeStep(String configName, String stepId){
        return new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.isOk())
                .get(configName, stepId);
    }

    public static BuildTypeStepsModel createBuildTypeStep(String configName){
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep",3))
                .type("simpleRunner")
                .build();

        return new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);
    }
}
