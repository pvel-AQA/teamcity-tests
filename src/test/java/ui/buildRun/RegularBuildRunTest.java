package ui.buildRun;

import api.comparison.ModelAssertions;
import api.enums.build.BuildState;
import api.enums.build.BuildStatus;
import api.enums.build.BuildStepCommand;
import api.models.build.BuildConfigurationResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ui.base.SingleThreadBaseTest;
import ui.pages.EditBuildConfigurationPage;

import static api.steps.UserSteps.createBuildConfigurationWithSteps;

public class RegularBuildRunTest extends SingleThreadBaseTest {

    @Test
    @Disabled("Disabled as it is flaky")
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void buildRunTest() {
        var buildConfigResponse = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);

        var buildRunId = new EditBuildConfigurationPage()
                .open(buildConfigResponse.getId())
                .runBuild()
                .checkIsStatus(BuildStatus.RUNNING)
                .checkIsStatus(BuildStatus.SUCCESS)
                .getBuildRunId();

        var buildRunResponse = UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(BuildStatus.SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);

        ModelAssertions.assertThatModels(buildConfigResponse, buildRunResponse).match();
    }
}
