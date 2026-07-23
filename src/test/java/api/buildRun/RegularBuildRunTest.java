package api.buildRun;

import api.enums.buildStates.BuildState;
import api.enums.buildStates.BuildStatus;
import api.enums.buildStates.BuildStatusText;
import api.enums.locators.LocatorType;
import api.generators.TeamCityDataGenerator;
import api.models.build.*;
import api.models.user.PropertiesContainer;
import api.models.user.Property;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import common.helpers.WaitUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Duration;
import java.util.List;

import static common.configs.Config.ADMIN_TOKEN;

@Execution(ExecutionMode.SAME_THREAD)
public class RegularBuildRunTest extends BaseTest {

    private static final String[] IGNORED_BUILD_FIELDS = {"href", "state", "status", "webUrl", "statusText"};

    @Test
    public void test() {
        var buildResponse = UserSteps.createBuildConfiguration();
        String buildConfigId = buildResponse.getId();

        // ==========================================
        // ШАГ 2. СОЗДАНИЕ БИЛД СТЕПА ЧЕРЕЗ DTO
        // ==========================================

        // Собираем свойства для шага (какую команду выполнить)
        var stepProperties = PropertiesContainer.builder()
                .property(List.of(
                        Property.builder().name("script.content").value("echo 'Hello World!'").build(),
                        Property.builder().name("use.custom.script").value("true").build()
                ))
                .build();

        // Собираем сам шаг
        var buildStep = BuildStepRequest.builder()
                .name("My Custom Command Line Step")
                .type("simpleRunner") // simpleRunner означает Command Line в TeamCity
                .properties(stepProperties)
                .build();

        // Отправляем шаг в TeamCity
        RestAssured.given()
                .auth().preemptive().basic("admin", "admin") // укажите ваши креды
                .contentType(ContentType.JSON)
                .body(buildStep) // Передаем DTO объект напрямую! Rest Assured сам превратит его в JSON
                .when()
                .post("http://localhost:8111/app/rest/buildTypes/id:" + buildConfigId + "/steps")
                .then()
                .statusCode(200);


        // ==========================================
        // ШАГ 3. ЗАПУСК СБОРКИ ЧЕРЕЗ DTO
        // ==========================================

        BuildRunRequest buildRunRequest = TeamCityDataGenerator.generateBuildRun(buildConfigId);

        // Отправляем билд в очередь на выполнение

        BuildRunResponse startedBuildRunResponse = new ValidatedCrudRequester<BuildRunResponse>
                (RequestSpec.adminSpec(ADMIN_TOKEN),
                        Endpoint.BUILD_QUEUE,
                        ResponseSpec.returnsOk())
                .post(buildRunRequest);
        var buildId = startedBuildRunResponse.getId();

        BuildRunResponse finishedBuildRunResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                RequestSpec.adminSpec(ADMIN_TOKEN),
                                Endpoint.BUILD_QUEUE_ITEM,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> res.getState().equals(BuildState.FINISHED),
                Duration.ofSeconds(20), Duration.ofSeconds(2));

// Главная проверка: статус должен быть SUCCESS
//        assertEquals(finalStatus, "SUCCESS", "Билд завершился с ошибкой или упал!");

        softy.assertThat(finishedBuildRunResponse.getStatus()).isEqualTo(BuildStatus.SUCCESS);
        softy.assertThat(finishedBuildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        Assertions.assertThat(startedBuildRunResponse)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_BUILD_FIELDS)
                .isEqualTo(finishedBuildRunResponse);
    }

    @Test
    public void testBuildStop() {
        var buildResponse = UserSteps.createBuildConfiguration();
        String buildConfigId = buildResponse.getId();

        var stepProperties = PropertiesContainer.builder()
                .property(List.of(
                        Property.builder().name("script.content").value("sleep 10").build(),
                        Property.builder().name("use.custom.script").value("true").build()
                ))
                .build();
        var buildStep = BuildStepRequest.builder().name("Step").type("simpleRunner").properties(stepProperties).build();

        RestAssured.given().auth().preemptive().basic("admin", "admin").contentType(ContentType.JSON).body(buildStep)
                .post("http://localhost:8111/app/rest/buildTypes/id:" + buildConfigId + "/steps");

        // Отправляем запрос с заголовком accept JSON, чтобы не ловить XML-ноды
        BuildRunRequest buildRunRequest = TeamCityDataGenerator.generateBuildRun(buildConfigId);

        // Отправляем билд в очередь на выполнение

        BuildRunResponse startedBuildRunResponse = new ValidatedCrudRequester<BuildRunResponse>
                (RequestSpec.adminSpec(ADMIN_TOKEN),
                        Endpoint.BUILD_QUEUE,
                        ResponseSpec.returnsOk())
                .post(buildRunRequest);
        var buildId = startedBuildRunResponse.getId();

        // Ждем, пока билд перейдет в состояние "running" на агенте
        BuildRunResponse runingBuildRunResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                RequestSpec.adminSpec(ADMIN_TOKEN),
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> res.getState().equals(BuildState.RUNNING),
                Duration.ofSeconds(20), Duration.ofSeconds(2));

        BuildCancelRequest cancelRequest = TeamCityDataGenerator.generateBuildCancel();

        // ДЕЙСТВИЕ: Останавливаем запущенный билд

        var cancelBuildRunResponse = new ValidatedCrudRequester<BuildCancelResponse>
                (RequestSpec.adminSpec(ADMIN_TOKEN),
                        Endpoint.BUILD_CANCEL,
                        ResponseSpec.returnsOk())
                .post(cancelRequest, LocatorType.ID + buildId);

        BuildRunResponse finalResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                RequestSpec.adminSpec(ADMIN_TOKEN),
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),

                res -> res.getState().equals(BuildState.FINISHED),
                Duration.ofSeconds(20), Duration.ofSeconds(2));

        // ПРОВЕРКА: Проверяем финал
        softy.assertThat(finalResponse.getState()).isEqualTo(BuildState.FINISHED);
        softy.assertThat(finalResponse.getStatus()).isEqualTo(BuildStatus.UNKNOWN);

        softy.assertThat(finalResponse.getCanceledInfo().getText()).isEqualTo(cancelRequest.getComment());
        softy.assertThat(finalResponse.getCanceledInfo().getUser().getUsername()).isEqualTo("admin");

        softy.assertThat(finalResponse.getStatusText())
                .contains(BuildStatusText.CANCELED.getValue());
        softy.assertThat(finalResponse.getStatusText())
                .contains(BuildStatusText.COMMAND_LINE_SIGTERM.getValue());


        softy.assertThat(finalResponse.getId()).isEqualTo(startedBuildRunResponse.getId());
        softy.assertThat(finalResponse.getBuildTypeId()).isEqualTo(startedBuildRunResponse.getBuildTypeId());
        softy.assertThat(finalResponse.getBuildType().getId()).isEqualTo(startedBuildRunResponse.getBuildType().getId());
    }

    @Test
    public void testBuildResume() throws InterruptedException {
        // 1. ПОДГОТОВКА: Создаем конфигурацию и шаг сборки через API
        var buildResponse = UserSteps.createBuildConfiguration();
        String buildConfigId = buildResponse.getId();

        var stepProperties = PropertiesContainer.builder()
                .property(List.of(
                        Property.builder().name("script.content").value("echo 'Action Resume worked!'").build(),
                        Property.builder().name("use.custom.script").value("true").build()
                ))
                .build();

        var buildStep = BuildStepRequest.builder()
                .name("Step")
                .type("simpleRunner")
                .properties(stepProperties)
                .build();

        RestAssured.given()
                .auth().preemptive().basic("admin", "admin")
                .contentType(ContentType.JSON)
                .body(buildStep)
                .when()
                .post("http://localhost:8111/app/rest/buildTypes/id:" + buildConfigId + "/steps")
                .then()
                .statusCode(200);


        // 2. ДЕЙСТВИЕ 1: Включаем ГЛОБАЛЬНУЮ паузу очереди через твой эндпоинт
        // Шлем JSON {"paused": true, "reason": "..."} методом PUT
        var pauseQueueRequest = BuildQueuePausedRequest.builder()
                .paused(true)
                .reason("Запрет ручного и автоматического запуска")
                .build();

        new CrudRequester(
                RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_QUEUE_PAUSED_STATE,
                ResponseSpec.returnsNoContent()) // На PUT TeamCity обычно отвечает 204 No Content
                .put(pauseQueueRequest, "");


        // 3. ДЕЙСТВИЕ 2: Отправляем сборку в queue
        BuildRunRequest buildRunRequest = TeamCityDataGenerator.generateBuildRun(buildConfigId);

        BuildRunResponse startedBuildRunResponse = new ValidatedCrudRequester<BuildRunResponse>(
                RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_QUEUE,
                ResponseSpec.returnsOk())
                .post(buildRunRequest);
        var buildId = startedBuildRunResponse.getId();


        // 4. ВАЖНАЯ ПРОВЕРКА: Ждем, когда сборка гарантированно отобразится в очереди со статусом QUEUED
        // Очередь на глобальной паузе, поэтому билд железно застрянет здесь
        BuildRunResponse initialQueueResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                RequestSpec.adminSpec(ADMIN_TOKEN),
                                Endpoint.BUILD_QUEUE_ITEM,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> res.getState().equals(BuildState.QUEUED),
                Duration.ofSeconds(10), Duration.ofMillis(500));

        softy.assertThat(initialQueueResponse.getState()).isEqualTo(BuildState.QUEUED);

        Thread.sleep(10000);


        // 5. ДЕЙСТВИЕ 3: Снимаем ГЛОБАЛЬНУЮ паузу с очереди (Resume)
        // Шлем JSON {"paused": false} методом PUT
        var resumeQueueRequest = BuildQueuePausedRequest.builder()
                .paused(false)
                .build();

        new CrudRequester(
                RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_QUEUE_PAUSED_STATE,
                ResponseSpec.returnsNoContent())
                .put(resumeQueueRequest, "");


        // 6. ОЖИДАНИЕ: Ждем, когда билд после снятия паузы успешно запустится и завершится (FINISHED)
        // Используем Endpoint.BUILD, так как из очереди билд сразу пропадает
        BuildRunResponse finalResponse = WaitUtils.waitFor(() ->
                        new ValidatedCrudRequester<BuildRunResponse>(
                                RequestSpec.adminSpec(ADMIN_TOKEN),
                                Endpoint.BUILD,
                                ResponseSpec.returnsOk())
                                .get(LocatorType.ID + buildId),
                res -> BuildState.FINISHED.equals(res.getState()),
                Duration.ofSeconds(30), Duration.ofSeconds(2));


        // 7. ФИНАЛЬНЫЕ ПРОВЕРКИ
        softy.assertThat(finalResponse.getState()).isEqualTo(BuildState.FINISHED);
        softy.assertThat(finalResponse.getStatus()).isEqualTo(BuildStatus.SUCCESS);

        Assertions.assertThat(startedBuildRunResponse)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_BUILD_FIELDS)
                .ignoringFields("buildType.paused")
                .isEqualTo(finalResponse);
    }
}

