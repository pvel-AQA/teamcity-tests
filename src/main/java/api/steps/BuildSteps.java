package api.steps;

import api.enums.locators.LocatorType;
import api.generators.RandomDataGenerator;
import api.models.BuildTypeStepsModel;
import api.models.build.Build;
import api.models.build.BuildRequest;
import api.models.build.StepProperties;
import api.models.project.PropertyItem;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import common.helpers.StepLogger;
import common.helpers.WaitUtils;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;

public class BuildSteps {

    public static final String STEP_OUTPUT_MARKER = "Hello from E2E build";

    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(3);

    public static BuildTypeStepsModel createRunnableStep(String configName) {
        BuildTypeStepsModel stepRequest = BuildTypeStepsModel.builder()
                .name(RandomDataGenerator.randomSpecificString("AutoStep", 3))
                .type("simpleRunner")
                .properties(StepProperties.builder()
                        .property(List.of(
                                PropertyItem.builder().name("use.custom.script").value("true").build(),
                                PropertyItem.builder().name("script.content").value("echo '" + STEP_OUTPUT_MARKER + "'").build()
                        ))
                        .build())
                .build();

        return new ValidatedCrudRequester<BuildTypeStepsModel>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_STEP_CREATE,
                ResponseSpec.returnsOk())
                .post(stepRequest, configName);
    }

    public static Build triggerBuild(String buildTypeId) {
        BuildRequest buildRequest = BuildRequest.builder()
                .buildType(BuildRequest.BuildType.builder().id(buildTypeId).build())
                .build();

        return new ValidatedCrudRequester<Build>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD_QUEUE,
                ResponseSpec.returnsOk())
                .post(buildRequest);
    }

    public static Build getBuild(int buildId) {
        return new ValidatedCrudRequester<Build>(
                RequestSpec.basicAuthSpec(),
                Endpoint.BUILD,
                ResponseSpec.returnsOk())
                .get(LocatorType.ID.getPrefix() + buildId);
    }

    public static Build waitForBuildToFinish(int buildId) {
        return StepLogger.log("Wait for build " + buildId + " to finish", () ->
                WaitUtils.waitForResult(
                        () -> getBuild(buildId),
                        build -> "finished".equals(build.getState()),
                        BUILD_TIMEOUT));
    }

    public static String getBuildLog(int buildId) {
        return StepLogger.log("Download build log for build " + buildId, () ->
                given()
                        .spec(RequestSpec.basicAuthSpec())
                        .when()
                        .get("/downloadBuildLog.html?buildId=" + buildId)
                        .then()
                        .extract()
                        .asString());
    }
}
