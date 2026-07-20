package api;

import api.comparison.ModelAssertions;
import api.generators.RandomGenerator;
import api.models.build.BuildTypeStepsModel;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;

public class ConfigStepsTest extends BaseTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void ConfigStepSuccessfullyCreatedTest() {
        String configName = UserSteps.createBuildConfiguration().getName();

        BuildTypeStepsModel createStepRequest = BuildTypeStepsModel.builder()
                .name(RandomGenerator.generateString("AutoStep", 3))
                .type("simpleRunner")
                .build();
        BuildTypeStepsModel createStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsOk())
                .post(createStepRequest,configName);

        ModelAssertions.assertThatModels(createStepResponse, createStepRequest).match();

        softly.assertThat(createStepResponse.getId())
                .as("Server assigns an id to a created step")
                .isNotBlank();

        String createdStepId = createStepResponse.getId();
        BuildTypeStepsModel getStepResponse = new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_STEP_READ,
                ResponseSpec.returnsOk())
                .get(configName, createdStepId);

        ModelAssertions.assertThatModels(createStepResponse, getStepResponse).match();
    }
}
