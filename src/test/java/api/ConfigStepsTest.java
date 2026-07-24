package api;

import api.comparison.ModelAssertions;
import api.enums.errors.StepErrors;
import api.generators.RandomGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.build.BuildTypeStepsList;
import api.models.build.BuildTypeStepsModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static api.enums.errors.AuthErrorMessage.*;

public class ConfigStepsTest extends BaseTest {
    private static final String STEP_TYPE_SIMPLE_RUNNER = "simpleRunner";
    private static final String STEP_NOT_FOUND_MESSAGE =
            "No step with id '%s' is found  in the build configuration.";

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void configStepSuccessfullyCreatedTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(TeamCityDataGenerator.generateString(9))
                .type(STEP_TYPE_SIMPLE_RUNNER)
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsOk())
                .post(createStepRequest,configName);

        ModelAssertions.assertThatModels(createStepResponse, createStepRequest).match();

        softly.assertThat(createStepResponse.getId()).isNotBlank();

        String createdStepId = createStepResponse.getId();
        BuildTypeStepsModel getStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_READ,
                ResponseSpec.returnsOk())
                .get(configName, createdStepId);

        ModelAssertions.assertThatModels(createStepResponse, getStepResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void configStepSuccessfullyUpdateNameTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        BuildTypeStepsModel createdStep =  UserSteps.createBuildTypeStep(configName, STEP_TYPE_SIMPLE_RUNNER);
        BuildTypeStepsModel updateStepRequest = BuildTypeStepsModel.builder()
                .name("UPDATED_"+ createdStep.getName())
                .type(STEP_TYPE_SIMPLE_RUNNER)
                .build();

        BuildTypeStepsModel updateStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsOk())
                .put(updateStepRequest,configName,createdStep.getId());

        softly.assertThat(updateStepResponse.getName())
                .isEqualTo(updateStepRequest.getName());

        BuildTypeStepsModel checkUpdatedStep = UserSteps.getBuildTypeStep(configName,createdStep.getId());
        ModelAssertions.assertThatModels(updateStepResponse, checkUpdatedStep).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void configStepSuccessfullyUpdateTypeTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        BuildTypeStepsModel createdStep =  UserSteps.createBuildTypeStep(configName, STEP_TYPE_SIMPLE_RUNNER);
        BuildTypeStepsModel updateStepRequest = BuildTypeStepsModel.builder()
                .name("UPDATED_"+ createdStep.getName())
                .type("NewType")
                .build();

        BuildTypeStepsModel updateStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsOk())
                .put(updateStepRequest,configName,createdStep.getId());

        softly.assertThat(updateStepResponse.getName())
                .isEqualTo(updateStepRequest.getName());

        BuildTypeStepsModel checkUpdatedStep = UserSteps.getBuildTypeStep(configName,createdStep.getId());
        ModelAssertions.assertThatModels(updateStepResponse, checkUpdatedStep).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void configStepSuccessfulDeleteTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String createdStepId = UserSteps.createBuildTypeStep(configName, STEP_TYPE_SIMPLE_RUNNER).getId();

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsDeleted())
                .delete(configName,createdStepId);

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsNotFound())
                .get(configName, createdStepId);

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsOk())
                .get(configName);
        softly.assertThat(steps.getCount()).isZero();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void configStepWithoutNameSuccessfullyCreatedTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .type(STEP_TYPE_SIMPLE_RUNNER)
                .build();

        BuildTypeStepsModel createStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsOk())
                .post(createStepRequest, configName);

        String createdStepId = createStepResponse.getId();
        softly.assertThat(createdStepId).isNotBlank();

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsOk())
                .get(configName);
        softly.assertThat(steps.getStep())
                .extracting(BuildTypeStepsModel::getId)
                .containsExactly(createdStepId);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void configStepWithoutTypeCannotBeCreatedTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(TeamCityDataGenerator.generateString(5))
                .build();

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsBadRequest(StepErrors.STEP_TYPE_CANNOT_BE_EMPTY.getErrorMsg()))
                .post(createStepRequest, configName);

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsOk())
                .get(configName);

        softly.assertThat(steps.getCount()).isEqualTo(0);
    }

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void cannotGetNonExistingStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String nonExistingStepId = TeamCityDataGenerator.generateString(8);

        ValidatableResponse response = new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_READ,
                ResponseSpec.returnsNotFound())
                .get(configName, nonExistingStepId);

        /*Response rawResponse = response.extract().response();
        System.out.println("Status: " + rawResponse.statusCode());
        System.out.println("Content-Type: " + rawResponse.contentType());
        System.out.println("Body:");
        System.out.println(rawResponse.asString());
        String actualMessage = response.extract()
                .xmlPath()
                .getString("errors.error.message");
        String expectedMessage = STEP_NOT_FOUND_MESSAGE.formatted(nonExistingStepId);
        softly.assertThat(actualMessage+"!").isEqualTo(expectedMessage);*/
    }

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void cannotUpdateNonExistingStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String nonExistingStepId = TeamCityDataGenerator.generateString("NoStep", 8);
        BuildTypeStepsModel updateStepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("");

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsNotFound())
                .put(updateStepRequest, configName, nonExistingStepId);

        //check there is no step with this name
    }

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void cannotDeleteNonExistingStepTest() {
        String configName = UserSteps.createBuildConfiguration().getName();
        String nonExistingStepId = TeamCityDataGenerator.generateString("NoStep", 8);

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsNotFound())
                .delete(configName, nonExistingStepId);

        //check error response?
    }

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void cannotCreateStepForNonExistingConfigTest() {
        String nonExistingConfig = TeamCityDataGenerator.generateString("NoConfig", 8);
        BuildTypeStepsModel createStepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsNotFound())
                .post(createStepRequest, nonExistingConfig);
    }

    @Disabled("Assertions need to be added")
    @Test
    public void cannotCreateConfigStepWithoutAuthTest() {
        String configLocator = TeamCityDataGenerator.generateString("NoConfig", 8);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .post(stepRequest, configLocator);
    }

    @Disabled("Assertions need to be added")
    @Test
    public void cannotUpdateConfigStepWithoutAuthTest() {
        String configLocator = TeamCityDataGenerator.generateString("NoConfig", 8);
        String stepLocator = TeamCityDataGenerator.generateString("NoStep", 8);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .put(stepRequest, configLocator, stepLocator);
    }


    @Disabled("Assertions need to be added")  @Test
    public void cannotDeleteConfigStepWithoutAuthTest() {
        String configLocator = TeamCityDataGenerator.generateString("NoConfig", 8);
        String stepLocator = TeamCityDataGenerator.generateString("NoStep", 8);

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .delete(configLocator, stepLocator);
    }

    @Disabled("Assertions need to be added")
    @Test
    public void cannotGetConfigStepWithoutAuthTest() {
        String configLocator = TeamCityDataGenerator.generateString("NoConfig", 8);
        String stepLocator = TeamCityDataGenerator.generateString("NoStep", 8);

        new CrudRequester(
                RequestSpec.unAuth(),
                Endpoint.BUILD_STEP_READ,
                ResponseSpec.returnsUnauthorized(AUTHENTICATION_REQUIRED))
                .get(configLocator, stepLocator);
    }

    @Disabled("Assertions need to be added")
    @Test
    public void cannotCreateStepWithInvalidBasicCredentialsTest() {
        String configName = TeamCityDataGenerator.generateString(5);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest(TeamCityDataGenerator.generateString(9));

        new CrudRequester(
                RequestSpec.basicAuthSpec(
                        TeamCityDataGenerator.generateString(),
                        TeamCityDataGenerator.generateString()),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsUnauthorized(BASIC_AUTH_FAILED))
                .post(stepRequest, configName);

        BuildTypeStepsList steps = new ValidatedCrudRequester<BuildTypeStepsList>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEPS_READ,
                ResponseSpec.returnsNotFound())
                .get(configName);

        softly.assertThat(steps.getCount())
                .as("A viewer must not be able to create a step")
                .isEqualTo(0);
    }

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER)
    public void viewerCannotCreateStepTest() {
        String configName = UserSteps.createBuildConfiguration(RequestSpec.basicAuthSpec()).getName();

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

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER)
    public void viewerCannotUpdateStepTest() {
       /* String configName = UserSteps.createBuildConfiguration(RequestSpec.basicAuthSpec()).getName();
        BuildTypeStepsModel createdStep = UserSteps.createBuildTypeStep(RequestSpec.basicAuthSpec(), configName);

        BuildTypeStepsModel updateStepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("UPDATED_" + createdStep.getName());

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_UPDATE,
                ResponseSpec.returnsForbidden())
                .put(updateStepRequest, configName, createdStep.getId());

       /* BuildTypeStepsModel stepAfter = UserSteps.getBuildTypeStep(configName, createdStep.getId());
        softly.assertThat(stepAfter.getName())
                .as("A viewer must not be able to change the step name")
                .isEqualTo(createdStep.getName());
        softly.assertThat(stepAfter.getType())
                .as("A viewer must not be able to change the step type")
                .isEqualTo(createdStep.getType());*/
    }

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.PROJECT_VIEWER)
    public void viewerCannotDeleteStepTest() {
      /*  String configName = UserSteps.createBuildConfiguration(RequestSpec.basicAuthSpec()).getName();
        String createdStepId = UserSteps.createBuildTypeStep(RequestSpec.basicAuthSpec(), configName).getId();

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsForbidden())
                .delete(configName, createdStepId);

        /*BuildTypeStepsModel stepAfter = UserSteps.getBuildTypeStep(configName, createdStepId);
        softly.assertThat(stepAfter.getId())
                .as("A viewer must not be able to delete the step")
                .isEqualTo(createdStepId);*/
    }

    @Disabled("Assertions need to be added")
    @Test
    @AuthUser(role = UserRoles.PROJECT_DEVELOPER)
    public void developerCannotDeleteStepTest() {
     /*   String configName = UserSteps.createBuildConfiguration(RequestSpec.basicAuthSpec()).getName();
        String createdStepId = UserSteps.createBuildTypeStep(RequestSpec.basicAuthSpec(), configName).getId();

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_DELETE,
                ResponseSpec.returnsForbidden())
                .delete(configName, createdStepId);

       /* BuildTypeStepsModel stepAfter = UserSteps.getBuildTypeStep(configName, createdStepId);
        softly.assertThat(stepAfter.getId())
                .as("A developer must not be able to delete the step")
                .isEqualTo(createdStepId);*/
    }

    @Disabled("Assertions need to be added")
    @Test
    public void cannotCreateStepWithInvalidBearerTokenTest() {
        String configLocator = TeamCityDataGenerator.generateString("NoConfig", 8);
        BuildTypeStepsModel stepRequest = TeamCityDataGenerator.generateBuildConfigurationStepRequest("AutoStep");

        new CrudRequester(
                RequestSpec.adminSpec(TeamCityDataGenerator.generateString()),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsUnauthorized(OAUTH_FAILED))
                .post(stepRequest, configLocator);
    }
}
