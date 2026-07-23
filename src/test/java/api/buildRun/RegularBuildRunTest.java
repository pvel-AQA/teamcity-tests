package api.buildRun;

import api.enums.build.BuildState;
import api.enums.build.BuildStatus;
import api.enums.build.BuildStatusText;
import api.enums.build.BuildStepCommand;
import api.enums.locators.LocatorType;
import api.generators.TeamCityDataGenerator;
import api.models.build.*;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import common.helpers.WaitUtils;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static api.steps.UserSteps.createBuildConfigurationWithSteps;
import static io.restassured.RestAssured.given;

@Execution(ExecutionMode.SAME_THREAD)
public class RegularBuildRunTest extends BaseTest {

    private static final String[] IGNORED_BUILD_FIELDS = {"href", "state", "status", "webUrl", "statusText"};
    private static final String PAUSED_FIELD = "buildType.paused";


    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    @Test
    public void testBuildRun() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);
        var buildConfigId = buildConfig.getId();

        BuildRunRequest buildRunRequest = TeamCityDataGenerator.generateBuildRun(buildConfigId);
        var startedBuildRunResponse = new ValidatedCrudRequester<BuildRunResponse>
                (RequestSpec.withAuthExtensionUser(),
                        Endpoint.BUILD_QUEUE,
                        ResponseSpec.returnsOk())
                .post(buildRunRequest);
        var buildId = startedBuildRunResponse.getId();

        RequestSpecification authSpec = RequestSpec.withAuthExtensionUser();
        BuildRunResponse finishedBuildRunResponse;

        try {
            finishedBuildRunResponse = WaitUtils.waitFor(() ->
                            new ValidatedCrudRequester<BuildRunResponse>(
                                    authSpec,
                                    Endpoint.BUILD,
                                    ResponseSpec.returnsOk())
                                    .get(LocatorType.ID + buildId),
                    res -> res.getState().equals(BuildState.FINISHED)
            );
        } catch (Throwable e) {
            System.out.println("=== НАЧАЛО ДЕБАГА ЗАВИСАНИЯ СБОРКИ ===");

            // 1. Смотрим, какие агенты сейчас вообще видит сервер и в каком они статусе
            String agents = given().spec(authSpec).get("/app/rest/agents?locator=authorized:any").asString();
            System.out.println("СОСТОЯНИЕ АГЕНТОВ НА СЕРВЕРЕ:\n" + agents);

            // 2. Смотрим совместимость конкретно этого билда с агентами (TeamCity сам пишет, почему они не подходят!)
            String compatibility = given().spec(authSpec).get("/app/rest/agents?locator=compatible:(build:(id:" + buildId + "))").asString();
            System.out.println("СОВМЕСТИМЫЕ АГЕНТЫ ДЛЯ БИЛДА:\n" + compatibility);

            // 3. Смотрим, какие шаги реально применились к этой конфигурации
            String buildTypeDetails = given().spec(authSpec).get("/app/rest/buildTypes/id:" + buildConfigId).asString();
            System.out.println("ЧТО СЕЙЧАС С ВНУТРЕННОСТЯМИ КОНФИГУРАЦИИ:\n" + buildTypeDetails);

            System.out.println("=== КОНЕЦ ДЕБАГА ===");
            throw e; // Пробрасываем ошибку дальше, чтобы тест честно упал
        }

        softly.assertThat(finishedBuildRunResponse.getStatus()).isEqualTo(BuildStatus.SUCCESS);
        softly.assertThat(finishedBuildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        Assertions.assertThat(startedBuildRunResponse)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_BUILD_FIELDS)
                .isEqualTo(finishedBuildRunResponse);
    }

    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    @Test
    public void testBuildStop() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);
        var buildConfigId = buildConfig.getId();

        BuildRunRequest buildRunRequest = TeamCityDataGenerator.generateBuildRun(buildConfigId);
        BuildRunResponse startedBuildRunResponse = new ValidatedCrudRequester<BuildRunResponse>
                (RequestSpec.withAuthExtensionUser(),
                        Endpoint.BUILD_QUEUE,
                        ResponseSpec.returnsOk())
                .post(buildRunRequest);
        var buildId = startedBuildRunResponse.getId();

        RequestSpecification authSpec = RequestSpec.withAuthExtensionUser();
        WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                authSpec,
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> res.getState().equals(BuildState.RUNNING)
        );

        BuildCancelRequest cancelRequest = TeamCityDataGenerator.generateBuildCancel();
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_CANCEL,
                ResponseSpec.returnsOk())
                .post(cancelRequest, LocatorType.ID + buildId);

        BuildRunResponse canceledBuildRunResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                authSpec,
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),

                res -> res.getState().equals(BuildState.FINISHED)
        );

        softly.assertThat(canceledBuildRunResponse.getState()).isEqualTo(BuildState.FINISHED);
        softly.assertThat(canceledBuildRunResponse.getStatus()).isEqualTo(BuildStatus.UNKNOWN);

        softly.assertThat(canceledBuildRunResponse.getCanceledInfo().getText()).isEqualTo(cancelRequest.getComment());

        softly.assertThat(canceledBuildRunResponse.getStatusText())
                .contains(BuildStatusText.CANCELED.getValue());


        softly.assertThat(canceledBuildRunResponse.getId()).isEqualTo(startedBuildRunResponse.getId());
        softly.assertThat(canceledBuildRunResponse.getBuildTypeId()).isEqualTo(startedBuildRunResponse.getBuildTypeId());
        softly.assertThat(canceledBuildRunResponse.getBuildType().getId()).isEqualTo(startedBuildRunResponse.getBuildType().getId());
    }

    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    @Test
    public void testBuildResume() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);
        var buildConfigId = buildConfig.getId();

        BuildQueuePausedRequest pauseQueueRequest = TeamCityDataGenerator.generateBuildQueuePausedRequest(true);

        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_QUEUE_PAUSED_STATE,
                ResponseSpec.returnsNoContent())
                .put(pauseQueueRequest);

        BuildRunRequest buildRunRequest = TeamCityDataGenerator.generateBuildRun(buildConfigId);

        var startedBuildRunResponse = new ValidatedCrudRequester<BuildRunResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_QUEUE,
                ResponseSpec.returnsOk())
                .post(buildRunRequest);

        String buildId = startedBuildRunResponse.getId();

        RequestSpecification authSpec = RequestSpec.withAuthExtensionUser();
        BuildRunResponse initialQueueResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                authSpec,
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> res.getState().equals(BuildState.QUEUED)
        );

        softly.assertThat(initialQueueResponse.getState()).isEqualTo(BuildState.QUEUED);

        BuildQueuePausedRequest resumeQueueRequest = TeamCityDataGenerator.generateBuildQueuePausedRequest(false);
        new CrudRequester(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_QUEUE_PAUSED_STATE,
                ResponseSpec.returnsNoContent())
                .put(resumeQueueRequest);

        BuildRunResponse finishedBuildRunResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                authSpec,
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> BuildState.FINISHED.equals(res.getState())
        );

        softly.assertThat(finishedBuildRunResponse.getState()).isEqualTo(BuildState.FINISHED);
        softly.assertThat(finishedBuildRunResponse.getStatus()).isEqualTo(BuildStatus.SUCCESS);

        Assertions.assertThat(startedBuildRunResponse)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_BUILD_FIELDS)
                .ignoringFields(PAUSED_FIELD)
                .isEqualTo(finishedBuildRunResponse);
    }

    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    @Test
    public void testBuildFailed() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.EXIT_WITH_ERROR);
        var buildConfigId = buildConfig.getId();

        BuildRunRequest buildRunRequest = TeamCityDataGenerator.generateBuildRun(buildConfigId);

        var startedBuildRunResponse = new ValidatedCrudRequester<BuildRunResponse>(
                RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_QUEUE,
                ResponseSpec.returnsOk())
                .post(buildRunRequest);
        var buildId = startedBuildRunResponse.getId();

        RequestSpecification authSpec = RequestSpec.withAuthExtensionUser();
        BuildRunResponse failedBuildRunResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                authSpec,
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> res.getState().equals(BuildState.FINISHED)
        );

        softly.assertThat(failedBuildRunResponse.getState()).isEqualTo(BuildState.FINISHED);
        softly.assertThat(failedBuildRunResponse.getStatus()).isEqualTo(BuildStatus.FAILURE);


        Assertions.assertThat(startedBuildRunResponse)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_BUILD_FIELDS)
                .isEqualTo(failedBuildRunResponse);
    }
}

