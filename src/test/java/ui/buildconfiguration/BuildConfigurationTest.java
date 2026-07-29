package ui.buildconfiguration;

import api.comparison.ModelAssertions;
import api.enums.buildconfiguration.BuildConfigDropdown;
import api.enums.buildconfiguration.BuildConfigTypeDropdown;
import api.generators.RandomGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.build.BuildConfigurationRequest;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import common.helpers.StepLogger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ui.BaseUiTest;
import ui.enums.successmessages.UISuccessMessage;
import ui.pages.EditBuildGeneralPage;
import ui.pages.EditBuildTypeVcsRootsPage;
import ui.pages.ProjectsPage;
import ui.pages.SetupYourBuildPage;

public class BuildConfigurationTest extends BaseUiTest {

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void buildConfigurationCanBeCreatedTest() {
        var project = UserSteps.createProject();
        var buildConfigurationRequest = RandomGenerator.generate(BuildConfigurationRequest.class);

        String configBuildName = new SetupYourBuildPage().open(project.getId())
                .clickBuildConfigurationButton()
                .selectOptionFromBuildConfigurationDropdown(BuildConfigDropdown.WITHOUT_REPOSITORY)
                .populateNameTextbox(buildConfigurationRequest.getName())
                .clickShowMoreButton()
                .selectOptionFromBuildConfigurationTypeDropdown(BuildConfigTypeDropdown.REGULAR)
                .clickCreateButton()

                .getPage(EditBuildTypeVcsRootsPage.class)
                .clickLastSelectedBuildTypeBreadcrumbs()
                .getBuildConfigNameText();

        String buildConfigIdText = new EditBuildGeneralPage().getBuildConfigIdText();
        buildConfigurationRequest.setId(buildConfigIdText);


        var listOfBuildConfigs = UserSteps.getBuilds().getBuildType().stream()
                .filter(build -> build.getId().equals(buildConfigIdText)).toList();
        Assertions.assertThat(listOfBuildConfigs).hasSize(1);
        var buildConfigApiResponse = listOfBuildConfigs.getFirst();

        softly.assertThat(buildConfigurationRequest.getName()).isEqualTo(configBuildName);
        ModelAssertions.assertThatModels(buildConfigApiResponse, buildConfigurationRequest);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void buildConfigurationCanBeCreatedFromProjectPageTest() {
        var project = UserSteps.createProject();
        var buildConfigurationRequest = RandomGenerator.generate(BuildConfigurationRequest.class);

        String configBuildNameInUI = StepLogger.log("Create Build configuration from SidePanel and" +
                " return Build Config name from Edit Build general page", () -> {
            return new ProjectsPage().open()
                    .getProjectsSideBar()
                    .clickCreateUnderProjectButton(project.getId())
                    .clickNewBuildConfigurationButtonFromPopup()

                    .clickBuildConfigurationButton()
                    .selectOptionFromBuildConfigurationDropdown(BuildConfigDropdown.WITHOUT_REPOSITORY)
                    .populateNameTextbox(buildConfigurationRequest.getName())
                    .clickShowMoreButton()
                    .selectOptionFromBuildConfigurationTypeDropdown(BuildConfigTypeDropdown.REGULAR)
                    .clickCreateButton()

                    .getPage(EditBuildTypeVcsRootsPage.class)
                    .clickLastSelectedBuildTypeBreadcrumbs()
                    .getBuildConfigNameText();
        });

        String buildConfigIdText = new EditBuildGeneralPage().getBuildConfigIdText();
        buildConfigurationRequest.setId(buildConfigIdText);


        var buildConfigApiResponse = StepLogger.log(
                "Validate via API that only one Build config was created and return it", () -> {
                    var listOfBuildConfigs = UserSteps.getBuilds().getBuildType().stream()
                            .filter(build -> build.getId().equals(buildConfigIdText)).toList();

                    Assertions.assertThat(listOfBuildConfigs).hasSize(1);
                    return listOfBuildConfigs.getFirst();
                });

        softly.assertThat(buildConfigurationRequest.getName()).isEqualTo(configBuildNameInUI);
        StepLogger.log("Validate that Build config returned from API has the same values as in UI", () -> {
            ModelAssertions.assertThatModels(buildConfigApiResponse, buildConfigurationRequest);
        });
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void buildConfigurationCanBeUpdatedTest() {
        var createdBuildConfiguration = UserSteps.createBuildConfiguration();
        final var updatedBuildConfigName = TeamCityDataGenerator.generateString();

        var buildConfigNameUIText = new ProjectsPage().open()
                .getProjectsSideBar()
                .expandProjectWithProjectIdIfRequired(createdBuildConfiguration.getProjectId())
                .clickBuildConfigurationWithBuildConfigId(createdBuildConfiguration.getId())
                .clickSettingsButton()
                .populateBuildConfigName(updatedBuildConfigName)
                .clickSaveButton()
                .checkSuccessMessageAppearsOnSavingChanges(UISuccessMessage.YOUR_CHANGES_HAVE_BEEN_SAVED)
                .getBuildConfigNameText();

        var buildConfigIdUIText = new EditBuildGeneralPage().getBuildConfigIdText();

        softly.assertThat(buildConfigNameUIText).isNotEqualTo(createdBuildConfiguration.getName());
        softly.assertThat(buildConfigNameUIText).isEqualTo(updatedBuildConfigName);
        softly.assertThat(buildConfigIdUIText).isEqualTo(createdBuildConfiguration.getId());

        var buildConfigurationResponse = UserSteps.getBuilds().getBuildType().stream()
                .filter(build -> build.getId().equals(buildConfigIdUIText))
                .toList().getFirst();

        softly.assertThat(buildConfigurationResponse.getName()).isEqualTo(buildConfigNameUIText);
        softly.assertThat(buildConfigurationResponse.getId()).isEqualTo(buildConfigIdUIText);
    }
}
