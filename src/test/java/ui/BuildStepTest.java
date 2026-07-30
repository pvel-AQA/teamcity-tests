package ui;

import api.generators.RandomGenerator;
import api.models.build.BuildConfigurationResponse;
import api.models.build.BuildTypeStepsList;
import api.models.build.BuildTypeStepsModel;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ui.base.BaseUiTest;
import ui.models.PowerShellUiModel;
import ui.pages.SetupYourBuildPage;
import ui.pages.buildsteps.BuildStepsPage;

import java.util.List;

import static com.codeborne.selenide.Condition.disabled;

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

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCanEditPowerShellBuildStepTest() {
        ProjectResponse projectResponse = UserSteps.createProject();
        BuildConfigurationResponse buildConfigurationResponse = UserSteps.createBuildConfiguration(projectResponse);
        PowerShellUiModel uiPowerShellStep = RandomGenerator.generate(PowerShellUiModel.class);
        uiPowerShellStep.setStepId(null);
        new SetupYourBuildPage()
                .open(projectResponse.getId());
        String newStepName = RandomStringUtils.insecure().nextAlphabetic(7);
        new BuildStepsPage()
                .open(buildConfigurationResponse.getId())
                .selectPowerShellRunner()
                .addBuildStep(uiPowerShellStep)
                .selectBuildStepByName(uiPowerShellStep.getStepName())
                .setStepNameValue(newStepName)
                .clickSaveButton()
                .isBuildStepVisible(uiPowerShellStep.getStepName() + newStepName);

        BuildTypeStepsModel apiBuildStepResponse = UserSteps
                .getBuildTypeStep(buildConfigurationResponse.getName(), uiPowerShellStep.getStepName());

        Assertions.assertThat(apiBuildStepResponse)
                .as("Assert that ui model fields equals to the api models")
                .matches(m -> m.getName().equals(uiPowerShellStep.getStepName() + newStepName));
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCannotChangeIdAfterCreatingPowerShellBuildStepTest() {
        ProjectResponse projectResponse = UserSteps.createProject();
        BuildConfigurationResponse buildConfigurationResponse = UserSteps.createBuildConfiguration(projectResponse);
        PowerShellUiModel uiPowerShellStep = RandomGenerator.generate(PowerShellUiModel.class);
        uiPowerShellStep.setStepId(null);
        new SetupYourBuildPage()
                .open(projectResponse.getId());
        new BuildStepsPage()
                .open(buildConfigurationResponse.getId())
                .selectPowerShellRunner()
                .addBuildStep(uiPowerShellStep)
                .selectBuildStepByName(uiPowerShellStep.getStepName())
                .getStepIdFieldInput().shouldBe(disabled);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void userCanDeletePowerShellBuildStepTest() {
        ProjectResponse projectResponse = UserSteps.createProject();
        BuildConfigurationResponse buildConfigurationResponse = UserSteps.createBuildConfiguration(projectResponse);
        PowerShellUiModel uiPowerShellStep = RandomGenerator.generate(PowerShellUiModel.class);
        uiPowerShellStep.setStepId(null);
        new SetupYourBuildPage()
                .open(projectResponse.getId());
        new BuildStepsPage()
                .open(buildConfigurationResponse.getId())
                .selectPowerShellRunner()
                .addBuildStep(uiPowerShellStep)
                .deleteBuildStep(uiPowerShellStep.getStepName())
                .acceptAlert(true)
                .isBuildStepNotVisible(uiPowerShellStep.getStepName());

        BuildTypeStepsList stepsList =
                UserSteps.getBuildTypeStepList(buildConfigurationResponse.getName());
        Assertions.assertThat(stepsList.getCount())
                .as("Assert that step doesn't exist: %s", uiPowerShellStep.getStepName())
                .isZero();
    }

}
