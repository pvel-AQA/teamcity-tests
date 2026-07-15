package api;

import api.comparison.ModelAssertions;
import api.enums.errors.StepErrors;
import api.generators.RandomDataGenerator;
import api.models.BuildTypeStepsList;
import api.models.BuildTypeStepsModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.Steps;
import base.BaseTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigStepsTest extends BaseTest {
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
        System.out.println("Project Name = " + projectName);
        String configName = Steps.createConfig(projectName).getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep",3))
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.isOk())
                .post(createStepRequest,configName);

        ModelAssertions.assertThatModels(createStepResponse, createStepRequest).match();

        String createdStepId = createStepResponse.getId();
        BuildTypeStepsModel getStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.isOk())
                .get(configName, createdStepId);

        ModelAssertions.assertThatModels(createStepResponse, getStepResponse).match();
    }

    @Test
    public void ConfigStepsSuccessfulUpdateTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        BuildTypeStepsModel createdStep =  Steps.createBuildTypeStep(configName);
        BuildTypeStepsModel updateStepRequest = BuildTypeStepsModel.builder()
                .name("UPDATED_"+ createdStep.getName())
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel updateStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.isOk())
                .put(updateStepRequest,configName,createdStep.getId());

        assertThat(updateStepRequest.getName()).isEqualTo(updateStepResponse.getName());

        BuildTypeStepsModel checkUpdatedStep = Steps.getBuildTypeStep(configName,createdStep.getId());
        ModelAssertions.assertThatModels(updateStepResponse, checkUpdatedStep).match();
    }

    @Test
    public void ConfigStepsSuccessfulDeleteTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        String createdStepId = Steps.createBuildTypeStep(configName).getId();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.deleted())
                .delete(configName,createdStepId);

        new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.notFound())
                .get(configName, createdStepId);
    }

    @Test
    public void ConfigStepsCanCreateStepWithoutNameTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatableCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.isOk())
                .post(createStepRequest, configName);

        String createdStepId = createStepResponse.getId();
        softy.assertThat(createdStepId)
                .as("A step created without a name should still receive an id")
                .isNotBlank();

        BuildTypeStepsList steps = new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.isOk())
                .get(configName)
                .extract().as(BuildTypeStepsList.class);

        softy.assertThat(steps.getStep())
                .as("The config should contain exactly the step that was just created")
                .extracting(BuildTypeStepsModel::getId)
                .containsExactly(createdStepId);
    }

    @Test
    public void ConfigStepsCannotCreateStepWithoutTypeTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep", 3))
                .build();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.isBadRequest(StepErrors.STEP_TYPE_CANNOT_BE_EMPTY.getErrorMsg()))
                .post(createStepRequest, configName);

        BuildTypeStepsList steps = new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.isOk())
                .get(configName)
                .extract().as(BuildTypeStepsList.class);

        softy.assertThat(steps.getCount())
                .as("A step without a type must not be created")
                .isEqualTo(0);
    }

    @Test
    public void ConfigStepsCannotGetNonExistingStepTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        String nonExistingStepId = RandomDataGenerator.randomSpecificString("NoStep", 8);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.notFound())
                .get(configName, nonExistingStepId);
    }

    @Test
    public void ConfigStepsCannotUpdateNonExistingStepTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        String nonExistingStepId = RandomDataGenerator.randomSpecificString("NoStep", 8);

        BuildTypeStepsModel updateStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep", 3))
                .type("simpleRunner")
                .build();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.notFound())
                .put(updateStepRequest, configName, nonExistingStepId);
    }

    @Test
    public void ConfigStepsCannotDeleteNonExistingStepTest() {
        String projectName = Steps.createProject().getName();
        String configName = Steps.createConfig(projectName).getName();
        String nonExistingStepId = RandomDataGenerator.randomSpecificString("NoStep", 8);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_RUD,
                ResponseSpec.notFound())
                .delete(configName, nonExistingStepId);
    }

    @Test
    public void ConfigStepsCannotCreateStepUnderNonExistingConfigTest() {
        String nonExistingConfig = RandomDataGenerator.randomSpecificString("NoConfig", 8);

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep", 3))
                .type("simpleRunner")
                .build();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.notFound())
                .post(createStepRequest, nonExistingConfig);
    }
}
