package api.e2e;

import api.generators.RandomGenerator;
import api.models.BuildTypeStepsModel;
import api.models.build.Build;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildConfigurationResponse;
import api.models.project.ProjectResponse;
import api.steps.UserSteps;
import base.BaseTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BasicFlowsTest extends BaseTest {

    @Disabled("Disabled till test not queued")
    @Test
    public void basicBuildFlowFromProjectToBuildLogTest() {
        ProjectResponse project = UserSteps.createProject();
        softly.assertThat(project.getId())
                .as("Authenticated admin should be able to create a project")
                .isNotBlank();

        BuildConfigurationRequest buildRequest = RandomGenerator.generate(BuildConfigurationRequest.class);
        buildRequest.getProject().setId(project.getId());
        BuildConfigurationResponse buildConfig = UserSteps.createBuildConfiguration(buildRequest);

        softly.assertThat(buildConfig.getProjectId())
                .as("Build configuration should be created under the project")
                .isEqualTo(project.getId());

        BuildTypeStepsModel step = UserSteps.createRunnableStep(buildConfig.getName());
        softly.assertThat(step.getId())
                .as("Configured build step should receive an id")
                .isNotBlank();

        Build queuedBuild = UserSteps.triggerBuild(buildConfig.getId());
        softly.assertThat(queuedBuild.getId())
                .as("Triggered build should be placed in the queue with an id")
                .isPositive();
        softly.assertThat(queuedBuild.getBuildTypeId())
                .as("Queued build should reference the triggered build configuration")
                .isEqualTo(buildConfig.getId());

        Build finishedBuild = UserSteps.waitForBuildToFinish(queuedBuild.getId());

        softly.assertThat(finishedBuild.getState())
                .as("Build should reach the finished state after execution")
                .isEqualTo("finished");
        softly.assertThat(finishedBuild.getAgent())
                .as("A finished build must have been assigned to an agent")
                .isNotNull();
        if (finishedBuild.getAgent() != null) {
            softly.assertThat(finishedBuild.getAgent().getName())
                    .as("The assigned agent should be identified by name")
                    .isNotBlank();
        }

        softly.assertThat(finishedBuild.getStatus())
                .as("Build with a successful echo step should finish as SUCCESS")
                .isEqualTo("SUCCESS");

        String buildLog = UserSteps.getBuildLog(finishedBuild.getId());
        softly.assertThat(buildLog)
                .as("Build log should be available")
                .isNotBlank();
        softly.assertThat(buildLog)
                .as("Build log should contain the executed step output")
                .contains(UserSteps.STEP_OUTPUT_MARKER);
    }
}
