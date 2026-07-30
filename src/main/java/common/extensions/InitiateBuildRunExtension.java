package common.extensions;

import api.models.build.BuildConfigurationResponse;
import api.steps.UserSteps;
import common.annotations.InititateBuildRun;
import common.helpers.EntityStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static api.enums.build.BuildStepCommand.ECHO_HELLO_WORLD;
import static api.steps.UserSteps.createBuildConfigurationWithSteps;
import static common.enums.TeamCityEntity.BUILD_CONFIGURATION;

public class InitiateBuildRunExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        InititateBuildRun annotation = context.getRequiredTestMethod().getAnnotation(InititateBuildRun.class);
        if (annotation != null) {
            BuildConfigurationResponse buildConfig = createBuildConfigurationWithSteps(ECHO_HELLO_WORLD);
            UserSteps.initiateBuildRun(buildConfig.getId());

            EntityStorage.addEntity(BUILD_CONFIGURATION.name(), buildConfig);
        }
    }
}
