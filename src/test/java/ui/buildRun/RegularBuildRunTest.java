package ui.buildRun;

import api.comparison.ModelAssertions;
import api.enums.build.BuildState;
import api.enums.build.BuildStepCommand;
import api.models.build.BuildConfigurationResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.annotations.InititateBuildRun;
import common.annotations.PauseBuildQueue;
import common.annotations.ResumeBuildQueueAfterTest;
import common.enums.UserRoles;
import common.helpers.EntityStorage;
import org.junit.jupiter.api.Test;
import ui.base.SingleThreadBaseTest;
import ui.pages.EditBuildConfigurationPage;
import ui.pages.QueuePage;

import static common.enums.BuildStatus.*;
import static api.enums.build.BuildStepCommand.ECHO_HELLO_WORLD;
import static api.enums.build.BuildStepCommand.EXIT_WITH_ERROR;
import static api.steps.UserSteps.createBuildConfigurationWithSteps;
import static common.enums.TeamCityEntity.BUILD_CONFIGURATION;

public class RegularBuildRunTest extends SingleThreadBaseTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void successfulBuildRunTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);

        var buildRunId = new EditBuildConfigurationPage()
                .open(buildConfig.getId())
                .runBuild()
                .waitUntilStatusBecomes(SUCCESS)
                .checkStatusBadge(SUCCESS)
                .openLogOverlayViaTimeline()
                .checkLogHeaderStatus(SUCCESS)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void runningBuildTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);

        var buildRunId = new EditBuildConfigurationPage()
                .open(buildConfig.getId())
                .runBuild()
                .waitUntilStatusBecomes(RUNNING)
                .checkStatusBadge(RUNNING)
                .checkStatusIndicator(RUNNING)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.RUNNING);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void failedBuildRunTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(EXIT_WITH_ERROR);

        var buildRunId = new EditBuildConfigurationPage()
                .open(buildConfig.getId())
                .runBuild()
                .waitUntilErrorStatusBecomes(EXIT_WITH_ERROR)
                .checkStatusBadge(FAILED)
                .openLogOverlayViaTimeline()
                .checkLogHeaderErrorStatus(EXIT_WITH_ERROR)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(FAILURE);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void canceledBuildRunTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(ECHO_HELLO_WORLD);

        var buildRunId = new EditBuildConfigurationPage()
                .open(buildConfig.getId())
                .runBuild()
                .stopBuildRun()
                .waitUntilStatusBecomes(CANCELED)
                .checkStatusBadge(CANCELED)
                .openLogOverlayViaTimeline()
                .checkLogHeaderStatus(CANCELED)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(UNKNOWN);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    @ResumeBuildQueueAfterTest
    public void pausedBuildRunTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(ECHO_HELLO_WORLD);

        var buildRunId = new QueuePage()
                .open()
                .pauseBuilds()
                .openProjectsPageViaMenu()
                .clickOnProject(buildConfig.getProjectName())
                .runBuild()
                .checkBuildStatusLinkIs(BUILD_QUEUE_WAS_PAUSED)
                .openBuild()
                .checkBuildStatusHeader(BUILD_QUEUE_WAS_PAUSED)
                .checkTimeLineStatus(IN_QUEUE)
                .openLogOverlayViaBuildLogMessages()
                .checkLogHeaderStatus(BUILD_QUEUE_WAS_PAUSED)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.QUEUED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    @PauseBuildQueue
    @InititateBuildRun
    public void resumeBuildRunTest() {
        BuildConfigurationResponse buildConfig = EntityStorage.getEntity(BUILD_CONFIGURATION.getName());

        var buildRunId = new QueuePage()
                .open()
                .checkBuildStatusText(BUILD_QUEUE_WAS_PAUSED)
                .resumeBuilds()
                .checkPausedStatusIsHiddenForBuild()
                .checkThereIsNoBuildsInQueue()
                .openProjectsPageViaMenu()
                .clickOnProject(buildConfig.getProjectName())
                .checkBuildStatusLinkIs(SUCCESS)
                .openBuild()
                .checkBuildStatusHeader(SUCCESS)
                .checkStatusBadge(SUCCESS)
                .openLogOverlayViaTimeline()
                .checkLogHeaderStatus(SUCCESS)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }
}
