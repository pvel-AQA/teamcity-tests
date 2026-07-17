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

/**
 * Steps for the runtime side of a build configuration: adding a runnable step,
 * putting a build into the queue, waiting for an agent to finish it, and reading its log.
 */
public class BuildSteps {

    public static final String STEP_OUTPUT_MARKER = "Hello from E2E build";

    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(3);

    /**
     * Creates a command-line ({@code simpleRunner}) step that runs a custom script,
     * so the triggered build actually executes and produces log output.
     * The build config name is used as the {@code btLocator}.
     */
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

    /**
     * Puts a build for the given build configuration id into the build queue.
     */
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

    /**
     * Polls the build until it reaches the {@code finished} state (an agent has picked it up and run it).
     */
    public static Build waitForBuildToFinish(int buildId) {
        return StepLogger.log("Wait for build " + buildId + " to finish", () ->
                WaitUtils.waitForResult(
                        () -> getBuild(buildId),
                        build -> "finished".equals(build.getState()),
                        BUILD_TIMEOUT));
    }

    /**
     * Downloads the plain-text build log. This endpoint lives at the server root,
     * not under the /app/rest prefix, so it is called directly.
     */
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
