package api.build;

import api.comparison.ModelAssertions;
import api.enums.locators.LocatorType;
import api.generators.RandomGenerator;
import api.generators.TeamCityDataGenerator;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildConfigurationResponse;
import api.models.build.CopyBuildConfigurationRequest;
import api.request.skelethon.Endpoint;
import api.request.skelethon.requester.CrudRequester;
import api.request.skelethon.requester.ValidatedCrudRequester;
import api.specs.RequestSpec;
import api.specs.ResponseSpec;
import api.steps.UserSteps;
import base.BaseTest;
import common.annotations.AuthUser;
import common.enums.UserRoles;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;


public class BuildConfigurationTest extends BaseTest {

    private static final String[] IGNORED_BUILD_FIELDS = {"project", "settings"};

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void userCanCreateBuildConfiguration() {
        var projectResponse = UserSteps.createProject();

        var buildRequest = RandomGenerator.generate(BuildConfigurationRequest.class);
        buildRequest.getProject().setId(projectResponse.getId());

        var buildResponse = new ValidatedCrudRequester<BuildConfigurationResponse>
                (RequestSpec.withAuthExtensionUser(),
                        Endpoint.BUILD_TYPES,
                        ResponseSpec.returnsOk())
                .post(buildRequest);


        var builds = new ValidatedCrudRequester<BuildConfigurationResponse>
                (RequestSpec.withAuthExtensionUser(),
                        Endpoint.BUILD_TYPES,
                        ResponseSpec.returnsOk())
                .get();

        var foundBuild = findBuild(builds, buildRequest.getId());

        ModelAssertions.assertThatModels(buildRequest, buildResponse).match();

        Assertions.assertThat(buildResponse)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_BUILD_FIELDS)
                .isEqualTo(foundBuild);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void userCanCopyBuildConfiguration() {
        var buildResponse = UserSteps.createBuildConfiguration();

        var copyBuildRequest = CopyBuildConfigurationRequest.createFrom(buildResponse);

        var copyBuildResponse = new ValidatedCrudRequester<BuildConfigurationResponse>
                (RequestSpec.withAuthExtensionUser(),
                        Endpoint.PROJECTS_BUILD_TYPES,
                        ResponseSpec.returnsOk())
                .post(copyBuildRequest, buildResponse.getProject().getId());

        var builds = UserSteps.getBuilds();

        var foundBuild = findBuild(builds, copyBuildRequest.getId());

        ModelAssertions.assertThatModels(copyBuildRequest, copyBuildResponse).match();

        Assertions.assertThat(copyBuildResponse)
                .usingRecursiveComparison()
                .ignoringFields(IGNORED_BUILD_FIELDS)
                .isEqualTo(foundBuild);
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void userCanDeleteBuildConfiguration() {
        var buildRequest = UserSteps.createBuildConfiguration();

        new CrudRequester(RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_TYPE,
                ResponseSpec.returnsNoContent())
                .delete(LocatorType.ID + buildRequest.getId());

        var builds = UserSteps.getBuilds();

        Assertions.assertThat(countBuildsWithId(builds, buildRequest.getId()))
                .as("Build with ID %s should be deleted", buildRequest.getId())
                .isZero();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void userCannotCreateBuildConfigurationWithDuplicateId() {
        var project = UserSteps.createProject();
        var firstBuild = TeamCityDataGenerator.generateBuildConfigurationFor(project.getId());

        UserSteps.createBuildConfiguration(firstBuild);

        var duplicateBuild = TeamCityDataGenerator.generateBuildConfigurationFor(project.getId());
        duplicateBuild.setId(firstBuild.getId());

        new CrudRequester(RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_TYPES,
                ResponseSpec.returnsBadRequest())
                .post(duplicateBuild);

        var builds = UserSteps.getBuilds();

        Assertions.assertThat(countBuildsWithId(builds, firstBuild.getId()))
                .as("Build should not be duplicate")
                .isOne();
    }

    @Test
    @AuthUser(role = UserRoles.SYSTEM_ADMIN)
    public void userCannotCreateBuildConfigurationWithInvalidId() {
        var invalidBuild = TeamCityDataGenerator.generateBuildConfigurationFor();

        invalidBuild.setId(RandomGenerator.generateString("_", 8));

        new CrudRequester(RequestSpec.withAuthExtensionUser(),
                Endpoint.BUILD_TYPES,
                ResponseSpec.returnsInternalServerError())
                .post(invalidBuild);

        var builds = UserSteps.getBuilds();

        Assertions.assertThat(countBuildsWithId(builds, invalidBuild.getId()))
                .as("Build should not be created")
                .isZero();
    }


    private static BuildConfigurationResponse findBuild(BuildConfigurationResponse buildCollection, String targetId) {
        if (buildCollection == null || buildCollection.getBuildType() == null) {
            throw new AssertionError("Build list is empty or null. Cannot find build with ID: " + targetId);
        }

        return buildCollection.getBuildType().stream()
                .filter(build -> build.getId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Build with ID " + targetId + " not founded"));
    }

    private static long countBuildsWithId(BuildConfigurationResponse builds, String targetId) {
        if (builds == null || builds.getBuildType() == null) return 0;
        return builds.getBuildType().stream()
                .filter(b -> b.getId().equals(targetId))
                .count();
    }

}
