package ui;

import api.generators.RandomGenerator;
import api.models.build.BuildConfigurationResponse;
import api.models.build.BuildTypeStepsModel;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ui.base.BaseUiTest;
import ui.models.PowerShellUiModel;
import ui.pages.SetupYourBuildPage;
import ui.pages.buildsteps.BuildStepsPage;

public class BuildStepTest extends BaseUiTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCanCreatePowerShellBuildStepTest() {
        ProjectResponse projectResponse = UserSteps.createProject();
        BuildConfigurationResponse buildConfigurationResponse = UserSteps.createBuildConfiguration(projectResponse);
        PowerShellUiModel uiPowerShellStep = RandomGenerator.generate(PowerShellUiModel.class);
        uiPowerShellStep.setStepId(null); // stepId = stepName after entering the name field
        new SetupYourBuildPage()
                .open(projectResponse.getId());
        new BuildStepsPage()
                .open(buildConfigurationResponse.getId())
                .selectPowerShellRunner()
                .addBuildStep(uiPowerShellStep)
                .isBuildStepVisible(uiPowerShellStep.getStepName());

        BuildTypeStepsModel apiBuildStepResponse = UserSteps
                .getBuildTypeStep(buildConfigurationResponse.getName(), uiPowerShellStep.getStepName());

        uiPowerShellStep.setStepId(uiPowerShellStep.getStepName());
        Assertions.assertThat(apiBuildStepResponse)
                .as("Assert that ui model fields equals to the api models")
                .matches(m -> m.getId().equals(uiPowerShellStep.getStepId()) &&
                        m.getName().equals(uiPowerShellStep.getStepName()) );
    }

}
