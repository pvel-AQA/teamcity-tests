package ui;

import api.models.build.BuildConfigurationResponse;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.junit.jupiter.api.Test;
import ui.pages.buildsteps.BuildStepsPage;

public class BuildStepTest extends BaseUiTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void someTest() {
        ProjectResponse projectResponse = UserSteps.createProject();
        BuildConfigurationResponse buildConfigurationResponse = UserSteps.createBuildConfiguration(projectResponse);

        new BuildStepsPage()
                .openBuildStepsTab(buildConfigurationResponse.getId())
                .selectPowerShellRunner()
                .addBuildStep();
    }

}
