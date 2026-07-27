package ui;

import api.generators.RandomGenerator;
import api.models.build.BuildConfigurationResponse;
import api.models.build.BuildTypeStepsModel;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import com.codeborne.selenide.Configuration;
import common.annotations.AuthUser;
import common.enums.PowerShellOptions;
import common.enums.UserRoles;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import ui.models.PowerShellUiModel;
import ui.pages.SetupYourBuildPage;
import ui.pages.buildsteps.BuildStepsPage;

public class BuildStepTest extends BaseUiTest {

    private static long oldPageLoadTimeout;

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCanCreatePowerShellBuildStepTest() {
        oldPageLoadTimeout = Configuration.pageLoadTimeout;
        Configuration.pageLoadTimeout = 60000;
        ProjectResponse projectResponse = UserSteps.createProject();
        BuildConfigurationResponse buildConfigurationResponse = UserSteps.createBuildConfiguration(projectResponse);
        PowerShellUiModel uiPowerShellStep = RandomGenerator.generate(PowerShellUiModel.class);
        uiPowerShellStep
                .setStepId(null)
                .setScript(PowerShellOptions.CODE)
                .setScriptSource("echo Hello_" + uiPowerShellStep.getScriptSource())
                .setScriptExecutionMode(null);

        new SetupYourBuildPage()
                .open(projectResponse.getId());
        boolean isBuildStepCreated = new BuildStepsPage()
                .open(buildConfigurationResponse.getId())
                .selectPowerShellRunner()
                .addBuildStep(uiPowerShellStep)
                .isBuildStepExists(uiPowerShellStep.getStepName());

        Assertions.assertThat(isBuildStepCreated)
                .isTrue()
                .as("Created build step should be displayed");

        BuildTypeStepsModel apiBuildStepResponse = UserSteps
                .getBuildTypeStep(buildConfigurationResponse.getName(), uiPowerShellStep.getStepName());

        uiPowerShellStep.setStepId(uiPowerShellStep.getStepName()); // stepId = stepName after entering
        Assertions.assertThat(apiBuildStepResponse)
                .as("Assert that ui model fields equals to the api models")
                .matches(m -> m.getId().equals(uiPowerShellStep.getStepId()) &&
                        m.getName().equals(uiPowerShellStep.getStepName()) );
    }

    @AfterAll
    public static void afterTest() {
        Configuration.pageLoadTimeout = oldPageLoadTimeout;
    }

}
