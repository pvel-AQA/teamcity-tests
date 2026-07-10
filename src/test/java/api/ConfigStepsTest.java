package api;

import api.models.BuildTypeStepsModel;
import api.models.projects.BuildTypeModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.Steps;
import org.junit.jupiter.api.Test;

public class ConfigStepsTest {
    //login
    //create project
    //create config
    //create step

    //+ step was created
    //-400 - missing/empty buildType, malformed JSON, or a bad field.
    //401 - wrong token or Auth: No Auth
    //403 - try to access with token of restricted user
    //404 - send in body non-existing info
    //500 - http://localhost:8111/app/rest/buildQueue/ with no body=>
    //Cannot read field "name" because "descriptor" is null
    //-

    @Test
    public void ConfigStepsCreatedTest() {
        String projectName = Steps.createProject().getName();
        String config = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name("Step1")
                .type("simpleRunner")
                .build();

        new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP,
                ResponseSpec.isOk())
                .post(createStepRequest);






        System.out.println(projectName);
        System.out.println(config);
    }
}
