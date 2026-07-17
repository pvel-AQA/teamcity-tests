package api.e2e;

import api.generators.RandomGenerator;
import api.models.BuildTypeStepsModel;
import api.models.build.Build;
import api.models.build.BuildConfigurationRequest;
import api.models.build.BuildConfigurationResponse;
import api.models.project.ProjectResponse;
import api.steps.BuildSteps;
import api.steps.UserSteps;
import base.BaseTest;
import org.junit.jupiter.api.Test;

/**
 * First end-to-end scenario covering the basic TeamCity build flow:
 * Authentication
 *   -> Project and build configuration access
 *   -> Build-step configuration
 *   -> Build queue
 *   -> Agent assignment
 *   -> Step execution
 *   -> Build result
 *   -> Build log
 * <p>
 * An authorized, connected agent is set up once in {@link BaseTest#setupAgent()}.
 */
public class BasicFlowsTest extends BaseTest {

    @Test
    public void basicBuildFlowFromProjectToBuildLogTest() {
        // 1. Authentication + project and build configuration access.
        //    Every requester below authenticates as admin; a failing login would surface here first.
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

        // 2. Build-step configuration: add a runnable command-line step to the configuration.
        BuildTypeStepsModel step = BuildSteps.createRunnableStep(buildConfig.getName());
        softly.assertThat(step.getId())
                .as("Configured build step should receive an id")
                .isNotBlank();

        // 3. Build queue: trigger a build for the configuration.
        Build queuedBuild = BuildSteps.triggerBuild(buildConfig.getId());
        softly.assertThat(queuedBuild.getId())
                .as("Triggered build should be placed in the queue with an id")
                .isPositive();
        softly.assertThat(queuedBuild.getBuildTypeId())
                .as("Queued build should reference the triggered build configuration")
                .isEqualTo(buildConfig.getId());

        // 4. Agent assignment + 5. Step execution: wait for an agent to pick up and run the build.
        Build finishedBuild = BuildSteps.waitForBuildToFinish(queuedBuild.getId());

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

        // 6. Build result: the executed step should succeed.
        softly.assertThat(finishedBuild.getStatus())
                .as("Build with a successful echo step should finish as SUCCESS")
                .isEqualTo("SUCCESS");

        // 7. Build log: the log should be available and contain the step output.
        String buildLog = BuildSteps.getBuildLog(finishedBuild.getId());
        softly.assertThat(buildLog)
                .as("Build log should be available")
                .isNotBlank();
        softly.assertThat(buildLog)
                .as("Build log should contain the executed step output")
                .contains(BuildSteps.STEP_OUTPUT_MARKER);
    }
}
