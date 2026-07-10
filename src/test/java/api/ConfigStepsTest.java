package api;

import api.comparison.ModelAssertions;
import api.generators.RandomDataGenerator;
import api.models.BuildTypeStepsModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.Steps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigStepsTest {
    //-400 - missing/empty buildType, malformed JSON, or a bad field.
    //401 - wrong token or Auth: No Auth
    //403 - try to access with token of restricted user
    //404 - send in body non-existing info
    //500 - http://localhost:8111/app/rest/buildQueue/ with no body=>
    //Cannot read field "name" because "descriptor" is null
    //-

    @Test
    public void ConfigStepsSuccessfullyCreatedTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep",3))
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        ModelAssertions.assertThatModels(createStepResponse, createStepRequest).match();

        String createdStepId = createStepResponse.getId();
        BuildTypeStepsModel getStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.isOk())
                .get(configName, createdStepId);

        ModelAssertions.assertThatModels(createStepResponse, getStepResponse).match();
    }

 /*   @Test
    public void ConfigStepsCreationNegativeTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name("Step18")
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        assertThat(createStepRequest.getName()).isEqualTo(createStepResponse.getName());

        //get stepID and compare response with createStepResponse
    }

    @Test
    public void ConfigStepsReadNegativeStatusTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name("Step18")
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        assertThat(createStepRequest.getName()).isEqualTo(createStepResponse.getName());

        //get stepID and compare response with createStepResponse
    }
//PUT
    @Test
    public void ConfigStepsSuccessfulUpdateTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name("Step18")
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        assertThat(createStepRequest.getName()).isEqualTo(createStepResponse.getName());

        //get stepID and compare response with createStepResponse
    }

    @Test
    public void ConfigStepsNegativeUpdateTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name("Step18")
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        assertThat(createStepRequest.getName()).isEqualTo(createStepResponse.getName());

        //get stepID and compare response with createStepResponse
    }

    //DELETE
    @Test
    public void ConfigStepsSuccessfulDeleteTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name("Step18")
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        assertThat(createStepRequest.getName()).isEqualTo(createStepResponse.getName());

        //get stepID and compare response with createStepResponse
    }

    @Test
    public void ConfigStepsNegativeTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name("Step18")
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.bearerSpec(),
                Endpoint.BUILD_STEP,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        assertThat(createStepRequest.getName()).isEqualTo(createStepResponse.getName());

        //get stepID and compare response with createStepResponse
    }*/
}
