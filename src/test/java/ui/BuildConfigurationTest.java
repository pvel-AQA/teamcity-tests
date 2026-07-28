package ui;

import api.comparison.ModelAssertions;
import api.enums.buildconfiguration.BuildConfigDropdown;
import api.enums.buildconfiguration.BuildConfigTypeDropdown;
import api.generators.RandomGenerator;
import api.models.build.BuildConfigurationRequest;
import api.steps.UserSteps;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ui.pages.EditBuildGeneralPage;
import ui.pages.EditBuildTypeVcsRootsPage;
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
}
