package ui.buildRun;

import api.enums.build.BuildState;
import api.enums.build.BuildStatus;
import api.enums.build.BuildStepCommand;
import api.models.build.BuildConfigurationResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import ui.base.SingleThreadBaseTest;
import ui.pages.EditBuildConfigurationPage;

import static api.steps.UserSteps.createBuildConfigurationWithSteps;

public class RegularBuildRunTest extends SingleThreadBaseTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void buildRunTest() {
        BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(BuildStepCommand.ECHO_HELLO_WORLD);

        var buildRunId = new EditBuildConfigurationPage()
                .open(buildConfig.getId())
                .runBuild()
                .checkIsStatus(BuildStatus.RUNNING)
                .checkIsStatus(BuildStatus.SUCCESS)
                .getBuildRunId();

       var buildRunResponse =  UserSteps.getBuildRunInfo(buildRunId);

        softly.assertThat(buildRunResponse.getStatus()).isEqualTo(BuildStatus.SUCCESS);
        softly.assertThat(buildRunResponse.getState()).isEqualTo(BuildState.FINISHED);
    }
}
