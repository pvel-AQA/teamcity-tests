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
import ui.base.BaseUiTest;
import ui.enums.alerts.BuildConfigAlert;
import ui.enums.errors.BuildConfigErrorMessage;
import ui.enums.successmessages.UISuccessMessage;
import ui.pages.*;

public class BuildConfigurationTest extends BaseUiTest {
    private static final int EXPECTED_NUMBER_OF_BUILD_CONFIGS_ONE = 1;

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
        Assertions.assertThat(listOfBuildConfigs).hasSize(EXPECTED_NUMBER_OF_BUILD_CONFIGS_ONE);
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

                    Assertions.assertThat(listOfBuildConfigs).hasSize(EXPECTED_NUMBER_OF_BUILD_CONFIGS_ONE);
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

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void buildConfigurationCanBeDeletedTest() {
        var createdBuildConfiguration = UserSteps.createBuildConfiguration();

        new ProjectsPage().open()
                .getProjectsSideBar()
                .expandProjectWithProjectIdIfRequired(createdBuildConfiguration.getProjectId())
                .clickBuildConfigurationWithBuildConfigId(createdBuildConfiguration.getId())
                .clickSettingsButton()
                .clickBuildConfigAdminActionsMenuButton()
                .clickDeleteBuildConfigButton()

                .checkAlertMessageAndAccept(BuildConfigAlert.DELETE_BUILD_CONFIG
                        .addBuildconfigNameToFormattedString(createdBuildConfiguration.getName()))

                .getPage(EditProjectPage.class)
                .checkSuccessMessageAppearsOnBuildConfigDeletion(
                        UISuccessMessage.BUILD_CONFIGURATION_DELETED, createdBuildConfiguration.getName());

        var buildConfigurationResponse = UserSteps.getBuilds().getBuildType().stream()
                .filter(build -> build.getId().equals(createdBuildConfiguration.getId()))
                .toList();

        Assertions.assertThat(buildConfigurationResponse.size()).isZero();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN, seedBrowserSession = true)
    public void buildConfigurationCanNotBeCreatedWithAlreadyExistingNameTest() {
        var createdBuildConfiguration = UserSteps.createBuildConfiguration();

        new SetupYourBuildPage().open(createdBuildConfiguration.getProjectId())
                .clickBuildConfigurationButton()
                .selectOptionFromBuildConfigurationDropdown(BuildConfigDropdown.WITHOUT_REPOSITORY)
                .populateNameTextbox(createdBuildConfiguration.getName())
                .clickCreateButton()

                .checkErrorNotificationAppearsOnCreationWithExistingName(
                        BuildConfigErrorMessage.BUILD_CONFIG_ALREADY_EXISTS,
                        createdBuildConfiguration.getName(), createdBuildConfiguration.getProjectName());

        var buildConfigurationResponse = UserSteps.getBuilds().getBuildType().stream()
                .filter(build -> build.getName().equals(createdBuildConfiguration.getName()))
                .toList();

        Assertions.assertThat(buildConfigurationResponse.size()).isEqualTo(EXPECTED_NUMBER_OF_BUILD_CONFIGS_ONE);
    }
}
