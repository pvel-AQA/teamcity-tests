package api;

import api.comparison.ModelAssertions;
import api.enums.errors.StepErrors;
import api.generators.RandomDataGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.BuildTypeStepsList;
import api.models.BuildTypeStepsModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;

import static api.enums.errors.AuthErrorMessage.*;
import static common.configs.Config.ADMIN_TOKEN;

public class ConfigStepsTest extends BaseTest {

    @Test
    public void ConfigStepsSuccessfullyCreatedTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep",3))
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsOk())
                .post(createStepRequest,configName);

        ModelAssertions.assertThatModels(createStepResponse, createStepRequest).match();

        softly.assertThat(createStepResponse.getId())
                .as("Server assigns an id to a created step")
                .isNotBlank();

        String createdStepId = createStepResponse.getId();
        BuildTypeStepsModel getStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_READ,
                ResponseSpec.returnsOk())
                .get(configName, createdStepId);

        ModelAssertions.assertThatModels(createStepResponse, getStepResponse).match();
    }

    @Test
    public void ConfigStepsSuccessfulUpdateTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        BuildTypeStepsModel createdStep =  UserSteps.createBuildTypeStep(configName);
        BuildTypeStepsModel updateStepRequest = BuildTypeStepsModel.builder()
                .name("UPDATED_"+ createdStep.getName())
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel updateStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsOk())
                .put(updateStepRequest,configName,createdStep.getId());

        softly.assertThat(updateStepResponse.getName())
                .as("Update changes the step name")
                .isEqualTo(updateStepRequest.getName());

        BuildTypeStepsModel checkUpdatedStep = UserSteps.getBuildTypeStep(configName,createdStep.getId());
        ModelAssertions.assertThatModels(updateStepResponse, checkUpdatedStep).match();
    }

    @Test
    public void ConfigStepsSuccessfulDeleteTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String createdStepId = UserSteps.createBuildTypeStep(configName).getId();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsDeleted())
                .delete(configName,createdStepId);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsNotFound())
                .get(configName, createdStepId);

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsOk())
                .get(configName);
        softly.assertThat(steps.getCount())
                .as("Step should be deleted")
                .isZero();
    }

    @Test
    public void ConfigStepsCanCreateStepWithoutNameTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .type("simpleRunner")
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsOk())
                .post(createStepRequest, configName);

        String createdStepId = createStepResponse.getId();
        softly.assertThat(createdStepId)
                .as("A step created without a name should still receive an id")
                .isNotBlank();

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsOk())
                .get(configName);

        softly.assertThat(steps.getStep())
                .as("The config should contain exactly the step that was just created")
                .extracting(BuildTypeStepsModel::getId)
                .containsExactly(createdStepId);
    }

    @Test
    public void ConfigStepsCannotCreateStepWithoutTypeTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep", 3))
                .build();

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsBadRequest(StepErrors.STEP_TYPE_CANNOT_BE_EMPTY.getErrorMsg()))
                .post(createStepRequest, configName);

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsOk())
                .get(configName);

        softly.assertThat(steps.getCount())
                .as("A step without a type must not be created")
                .isEqualTo(0);
    }

    @Test
    public void ConfigStepsCannotGetNonExistingStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String nonExistingStepId = RandomDataGenerator.randomSpecificString("NoStep", 8);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsNotFound())
                .get(configName, nonExistingStepId);
    }

    @Test
    public void ConfigStepsCannotUpdateNonExistingStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String nonExistingStepId = RandomDataGenerator.randomSpecificString("NoStep", 8);
        BuildTypeStepsModel updateStepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("");

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsNotFound())
                .put(updateStepRequest, configName, nonExistingStepId);
    }

    @Test
    public void ConfigStepsCannotDeleteNonExistingStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String nonExistingStepId = RandomDataGenerator.randomSpecificString("NoStep", 8);

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsNotFound())
                .delete(configName, nonExistingStepId);
    }

    @Test
    public void ConfigStepsCannotCreateStepUnderNonExistingConfigTest() {
        String nonExistingConfig = RandomDataGenerator.randomSpecificString("NoConfig", 8);
        BuildTypeStepsModel createStepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsNotFound())
                .post(createStepRequest, nonExistingConfig);
    }

    @Test
    public void ConfigStepsCannotCreateWithoutAuthTest() {
        String configLocator = RandomDataGenerator.randomSpecificString("NoConfig", 8);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .post(stepRequest, configLocator);
    }

    @Test
    public void ConfigStepsCannotUpdateWithoutAuthTest() {
        String configLocator = RandomDataGenerator.randomSpecificString("NoConfig", 8);
        String stepLocator = RandomDataGenerator.randomSpecificString("NoStep", 8);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .put(stepRequest, configLocator, stepLocator);
    }

    @Test
    public void ConfigStepsCannotDeleteWithoutAuthTest() {
        String configLocator = RandomDataGenerator.randomSpecificString("NoConfig", 8);
        String stepLocator = RandomDataGenerator.randomSpecificString("NoStep", 8);

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .delete(configLocator, stepLocator);
    }

    @Test
    public void ConfigStepsCannotGetStepWithoutAuthTest() {
        String configLocator = RandomDataGenerator.randomSpecificString("NoConfig", 8);
        String stepLocator = RandomDataGenerator.randomSpecificString("NoStep", 8);

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_READ,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .get(configLocator, stepLocator);
    }

    @Test
    public void ConfigStepsCannotCreateStepWithInvalidBasicCredentialsTest() {
        String configName = RandomDataGenerator.randomSpecificString("NoConfig", 8);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.basicAuthSpec(
                        RandomDataGenerator.randomString(10),
                        RandomDataGenerator.randomString(10)),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsUnauthorized(BASIC_AUTH_FAILED))
                .post(stepRequest, configName);

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsNotFound())
                .get(configName);

        /*softly.assertThat(steps.getCount())
                .as("A viewer must not be able to create a step")
                .isEqualTo(0);*/
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER)
    public void ConfigStepsViewerCannotCreateStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsForbidden())
                .post(createStepRequest, configName);

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsOk())
                .get(configName);

        softly.assertThat(steps.getCount())
                .as("A viewer must not be able to create a step")
                .isEqualTo(0);
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER)
    public void ConfigStepsViewerCannotUpdateStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        BuildTypeStepsModel createdStep = UserSteps.createBuildTypeStep(configName);

        BuildTypeStepsModel updateStepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("UPDATED_" + createdStep.getName());

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsForbidden())
                .put(updateStepRequest, configName, createdStep.getId());

        BuildTypeStepsModel stepAfter = UserSteps.getBuildTypeStep(configName, createdStep.getId());
        softly.assertThat(stepAfter.getName())
                .as("A viewer must not be able to change the step name")
                .isEqualTo(createdStep.getName());
        softly.assertThat(stepAfter.getType())
                .as("A viewer must not be able to change the step type")
                .isEqualTo(createdStep.getType());
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER)
    public void ConfigStepsViewerCannotDeleteStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String createdStepId = UserSteps.createBuildTypeStep(configName).getId();

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsForbidden())
                .delete(configName, createdStepId);

        BuildTypeStepsModel stepAfter = UserSteps.getBuildTypeStep(configName, createdStepId);
        softly.assertThat(stepAfter.getId())
                .as("A viewer must not be able to delete the step")
                .isEqualTo(createdStepId);
    }

    @Test
    @AuthUser(role = UserRoles.PROJECT_DEVELOPER)
    public void ConfigStepsDeveloperCannotDeleteStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String createdStepId = UserSteps.createBuildTypeStep(configName).getId();

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsForbidden())
                .delete(configName, createdStepId);

        BuildTypeStepsModel stepAfter = UserSteps.getBuildTypeStep(configName, createdStepId);
        softly.assertThat(stepAfter.getId())
                .as("A developer must not be able to delete the step")
                .isEqualTo(createdStepId);
    }

    @Test
    public void ConfigStepsCannotCreateStepWithInvalidBearerTokenTest() {
        String configLocator = RandomDataGenerator.randomSpecificString("NoConfig", 8);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.adminSpec(RandomDataGenerator.randomString(20)),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsUnauthorized(OAUTH_FAILED))
                .post(stepRequest, configLocator);
    }
}
