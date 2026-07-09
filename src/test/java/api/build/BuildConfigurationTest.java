package api.build;

import api.comparison.ModelAssertions;
import api.generators.RandomGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.BuildConfigurationRequest;
import api.models.BuildConfigurationResponse;
import api.models.CopyBuildConfigurationRequest;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatableCrudRequester;
import api.request.steps.AdminSteps;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static common.configs.Config.ADMIN_TOKEN;


public class BuildConfigurationTest {

    @Test
    public void userCanCreateBuildConfiguration() {
        var projectResponse = UserSteps.createProject();

        var buildRequest = RandomGenerator.generate(BuildConfigurationRequest.class);
        buildRequest.getProject().setId(projectResponse.getId());

        var buildResponse = new ValidatableCrudRequester<BuildConfigurationResponse>
                (RequestSpec.adminSpec(ADMIN_TOKEN),
                        Endpoint.BUILD_TYPES,
                        ResponseSpec.isOk())
                .post(buildRequest);


        var builds = new ValidatableCrudRequester<BuildConfigurationResponse>
                (RequestSpec.adminSpec(ADMIN_TOKEN),
                        Endpoint.BUILD_TYPES,
                        ResponseSpec.isOk())
                .get();

        var foundBuild = findBuild(builds, buildRequest.getId());

        ModelAssertions.assertThatModels(buildRequest, buildResponse).match();

        Assertions.assertThat(buildResponse)
                .usingRecursiveComparison()
                .ignoringFields("project", "settings")
                .isEqualTo(foundBuild);
    }

    @Test
    public void userCanCopyBuildConfiguration() {
        var buildResponse = UserSteps.createBuildConfiguration();

        var copyBuildRequest = CopyBuildConfigurationRequest.createFrom(buildResponse);

        var copyBuildResponse = new ValidatableCrudRequester<BuildConfigurationResponse>
                (RequestSpec.adminSpec(ADMIN_TOKEN),
                        Endpoint.PROJECTS_BUILD_TYPES,
                        ResponseSpec.isOk())
                .post(copyBuildRequest, buildResponse.getProject().getId());

        var builds = UserSteps.getBuilds();

        var foundBuild = findBuild(builds, copyBuildRequest.getId());

        ModelAssertions.assertThatModels(copyBuildRequest, copyBuildResponse).match();

        Assertions.assertThat(copyBuildResponse)
                .usingRecursiveComparison()
                .ignoringFields("project", "settings")
                .isEqualTo(foundBuild);
    }

    @Test
    public void userCanDeleteBuildConfiguration() {
        var buildRequest = UserSteps.createBuildConfiguration();

        new CrudRequester(RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_TYPES,
                ResponseSpec.isNoContent())
                .delete("id:" + buildRequest.getId());

        var builds = AdminSteps.getBuilds();

        Assertions.assertThat(builds.getBuildType())
                .extracting(BuildConfigurationResponse::getId)
                .withFailMessage("Build with ID " + buildRequest.getId() + " still exist after delete")
                .doesNotContain(buildRequest.getId());
    }

    @Test
    public void userCannotCreateBuildConfigurationWithDuplicateId() {
        var project = UserSteps.createProject();
        var firstBuild = TeamCityDataGenerator.generateBuildConfigurationFor(project.getId());

        UserSteps.createBuildConfiguration(firstBuild);

        var duplicateBuild = TeamCityDataGenerator.generateBuildConfigurationFor(project.getId());
        duplicateBuild.setId(firstBuild.getId());

        new CrudRequester(RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_TYPES,
                ResponseSpec.isBadRequest())
                .post(duplicateBuild);
    }

    @Test
    public void userCannotCreateBuildConfigurationWithInvalidId() {
        var invalidBuild = TeamCityDataGenerator.generateBuildConfigurationFor();

        invalidBuild.setId("_invalidId123");

        new CrudRequester(RequestSpec.adminSpec(ADMIN_TOKEN),
                Endpoint.BUILD_TYPES,
                ResponseSpec.isInternalServerError())
                .post(invalidBuild);
    }


    public static BuildConfigurationResponse findBuild(BuildConfigurationResponse buildCollection, String targetId) {
        if (buildCollection == null || buildCollection.getBuildType() == null) {
            throw new AssertionError("Build list is empty or null. Cannot find build with ID: " + targetId);
        }

        return buildCollection.getBuildType().stream()
                .filter(build -> build.getId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Build with ID " + targetId + " not founded"));
    }

}
