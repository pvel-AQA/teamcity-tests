package ui.buildRun;

import api.comparison.ModelAssertions;
import api.enums.build.BuildState;
import common.enums.BuildStepCommand;
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
import ui.pages.CreateProjectPage;
import ui.pages.QueuePage;
import ui.pages.EditBuildGeneralPage;

import static common.enums.BuildStatus.*;
import static common.enums.BuildStepCommand.ECHO_HELLO_WORLD;
import static common.enums.BuildStepCommand.EXIT_WITH_ERROR;
import static api.steps.UserSteps.createBuildConfigurationWithSteps;
import static common.enums.TeamCityEntity.BUILD_CONFIGURATION;

public class RegularBuildRunTest extends SingleThreadBaseTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void checkSuccessBuildStatusIsDisplayedTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);

        var buildRunId = new EditBuildGeneralPage()
                .open(buildConfig.getId())
                .runBuild()
                .waitUntilStatusBecomes(SUCCESS)
                .checkStatusBadgeIs(SUCCESS)
                .openLogOverlayViaTimeline()
                .checkLogHeaderStatusIs(SUCCESS)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void checkRunningBuildStatusIsDisplayedTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);

        var buildRunId = new CreateProjectPage()
                .open()
                .getPage(EditBuildGeneralPage.class)
                .open(buildConfig.getId())
                .runBuild()
                .waitUntilStatusBecomes(RUNNING)
                .checkStatusBadgeIs(RUNNING)
                .checkStatusIndicatorIs(RUNNING)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.RUNNING);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void checkFailedBuildStatusIsDisplayedTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(EXIT_WITH_ERROR);

        var buildRunId = new EditBuildGeneralPage()
                .open(buildConfig.getId())
                .runBuild()
                .waitUntilErrorStatusBecomes(EXIT_WITH_ERROR)
                .checkStatusBadgeIs(FAILED)
                .openLogOverlayViaTimeline()
                .checkLogHeaderErrorStatusIs(EXIT_WITH_ERROR)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(FAILURE);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void checkCanceledBuildStatusIsDisplayedTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(ECHO_HELLO_WORLD);

        var buildRunId = new EditBuildGeneralPage()
                .open(buildConfig.getId())
                .runBuild()
                .stopBuildRun()
                .waitUntilStatusBecomes(CANCELED)
                .checkStatusBadgeIs(CANCELED)
                .openLogOverlayViaTimeline()
                .checkLogHeaderStatusIs(CANCELED)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(UNKNOWN);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    @ResumeBuildQueueAfterTest
    public void checkPausedBuildStatusIsDisplayedTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(ECHO_HELLO_WORLD);

        var buildRunId = new QueuePage()
                .open()
                .pauseBuilds()
                .openProjectsPageViaMenu()
                .clickOnProject(buildConfig.getProjectName())
                .runBuild()
                .checkBuildStatusLinkIs(BUILD_QUEUE_WAS_PAUSED)
                .openBuild()
                .checkBuildStatusHeaderIs(BUILD_QUEUE_WAS_PAUSED)
                .checkTimeLineStatusIs(IN_QUEUE)
                .openLogOverlayViaBuildLogMessages()
                .checkLogHeaderStatusIs(BUILD_QUEUE_WAS_PAUSED)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.QUEUED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    @PauseBuildQueue
    @InititateBuildRun
    public void buildRunCanBeResumedAfterPauseTest() {
        BuildConfigurationResponse buildConfig = EntityStorage.getEntity(BUILD_CONFIGURATION.name());

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
                .checkBuildStatusHeaderIs(SUCCESS)
                .checkStatusBadgeIs(SUCCESS)
                .openLogOverlayViaTimeline()
                .checkLogHeaderStatusIs(SUCCESS)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfig, buildRunResponse).match();
    }
}
